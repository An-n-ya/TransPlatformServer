package com.app.interaction;

import com.app.common.PageResult;
import com.app.content.Post;
import com.app.content.PostRepository;
import com.app.notification.NotificationService;
import com.app.user.UserService;
import com.app.user.UserVO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.app.config.RabbitConfig.INTERACTION_EXCHANGE;
import static com.app.config.RabbitConfig.RK_COMMENT_CREATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    @CacheEvict(value = "post", key = "#request.postId")
    public CommentVO createComment(Long userId, CommentCreateRequest request) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new EntityNotFoundException("帖文不存在"));

        if (post.getStatus() == 0) {
            throw new EntityNotFoundException("帖文已被删除");
        }

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("父评论不存在"));
            if (!parent.getPostId().equals(request.getPostId())) {
                throw new IllegalArgumentException("评论与帖文不匹配");
            }
        }

        Comment comment = new Comment(
                request.getPostId(),
                userId,
                request.getParentId(),
                request.getReplyToUserId(),
                request.getContent()
        );
        comment = commentRepository.save(comment);

        // 更新帖文评论计数
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        // 通知帖主
        notificationService.createNotification(post.getUserId(), "comment",
                "评论了你的帖文", request.getContent(), userId, post.getId());

        // 如果是回复，通知父评论作者
        if (request.getParentId() != null && comment.getReplyToUserId() != null) {
            notificationService.createNotification(comment.getReplyToUserId(), "reply",
                    "回复了你的评论", request.getContent(), userId, comment.getParentId());
        }

        rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, RK_COMMENT_CREATED,
                new CommentEvent(comment.getId(), userId, request.getPostId()));

        log.info("Comment created: id={}, userId={}, postId={}", comment.getId(), userId, request.getPostId());
        return buildCommentVO(comment, userId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("评论不存在"));

        if (!comment.getUserId().equals(userId)) {
            throw new SecurityException("无权删除他人的评论");
        }

        doDeleteComment(comment);
        log.info("Comment deleted: id={}, userId={}", commentId, userId);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("评论不存在"));

        doDeleteComment(comment);
        log.info("Comment deleted by admin: id={}", commentId);
    }

    /** 评论逻辑删除（置 status=0，并同步递减帖文评论计数） */
    private void doDeleteComment(Comment comment) {
        if (comment.getStatus() == 0) {
            throw new EntityNotFoundException("评论已被删除");
        }

        comment.setStatus(0);
        commentRepository.save(comment);

        Post post = postRepository.findById(comment.getPostId()).orElse(null);
        if (post != null && post.getCommentsCount() > 0) {
            post.setCommentsCount(post.getCommentsCount() - 1);
            postRepository.save(post);
        }
    }

    @Override
    public PageResult<CommentVO> getPostComments(Long postId, Long currentUserId, Pageable pageable) {
        // 一次性取出该帖文所有正常评论
        List<Comment> allComments = commentRepository
                .findByPostIdAndStatusOrderByCreatedAtAsc(postId, 1);

        // 构建 parentId → children 映射
        Map<Long, List<Comment>> childrenMap = allComments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 提取顶级评论（parentId IS NULL）做分页
        List<Comment> roots = allComments.stream()
                .filter(c -> c.getParentId() == null)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), roots.size());
        if (start >= roots.size()) {
            return PageResult.empty();
        }

        List<CommentVO> vos = roots.subList(start, end).stream()
                .map(root -> {
                    // 递归收集所有子孙评论
                    List<Comment> descendants = collectDescendants(root.getId(), childrenMap);
                    long descCount = descendants.size();

                    // 选 top_reply：点赞数最高，同分取最新
                    CommentVO.CommentTopReply topReply = selectTopReply(descendants);

                    CommentVO vo = buildCommentVO(root, currentUserId);
                    vo.setCommentsCount(descCount);
                    vo.setTopReply(topReply);
                    return vo;
                })
                .toList();

        return PageResult.of(vos, pageable.getPageNumber(), pageable.getPageSize(), roots.size());
    }

    @Override
    public PageResult<CommentVO> getCommentReplies(Long commentId, Long currentUserId, Pageable pageable) {
        // 一次性获取该帖文所有评论，构建树，收集所有子孙
        Comment rootComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("评论不存在"));

        List<Comment> allComments = commentRepository
                .findByPostIdAndStatusOrderByCreatedAtAsc(rootComment.getPostId(), 1);

        Map<Long, List<Comment>> childrenMap = allComments.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        List<Comment> allDescendants = collectDescendants(commentId, childrenMap);

        List<CommentVO> vos = allDescendants.stream()
                .map(c -> buildCommentVO(c, currentUserId))
                .toList();

        return PageResult.of(vos, 0, vos.size(), vos.size());
    }

    /** 递归收集某个评论的所有子孙评论 */
    private List<Comment> collectDescendants(Long parentId, Map<Long, List<Comment>> childrenMap) {
        List<Comment> result = new ArrayList<>();
        List<Comment> directChildren = childrenMap.getOrDefault(parentId, Collections.emptyList());
        for (Comment child : directChildren) {
            result.add(child);
            result.addAll(collectDescendants(child.getId(), childrenMap));
        }
        return result;
    }

    /** 从子孙列表中选一条作为精选回复：点赞数最高→最新 */
    private CommentVO.CommentTopReply selectTopReply(List<Comment> descendants) {
        if (descendants.isEmpty()) return null;
        Comment best = descendants.stream()
                .max(Comparator.comparingInt(Comment::getLikesCount)
                        .thenComparing(Comment::getCreatedAt, Comparator.reverseOrder()))
                .orElse(null);
        if (best == null) return null;
        UserVO author = userService.getUserById(best.getUserId());
        return CommentVO.CommentTopReply.builder()
                .id(best.getId())
                .userId(best.getUserId())
                .nickname(author.getNickname())
                .content(best.getContent())
                .likesCount(best.getLikesCount())
                .build();
    }

    private CommentVO buildCommentVO(Comment comment, Long currentUserId) {
        UserVO author = userService.getUserById(comment.getUserId());
        UserVO replyToUser = comment.getReplyToUserId() != null
                ? userService.getUserById(comment.getReplyToUserId())
                : null;

        Boolean liked = null;
        if (currentUserId != null) {
            liked = likeRepository.existsByUserIdAndTargetTypeAndTargetId(
                    currentUserId, "comment", comment.getId());
        }

        return CommentVO.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .author(author)
                .parentId(comment.getParentId())
                .replyToUser(replyToUser)
                .content(comment.getContent())
                .likesCount(comment.getLikesCount())
                .liked(liked)
                .commentsCount(0L)
                .createdAt(comment.getCreatedAt())
                .build();
    }

    public record CommentEvent(Long commentId, Long userId, Long postId) {}
}
