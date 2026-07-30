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

import java.util.List;

import static com.app.config.RabbitConfig.INTERACTION_EXCHANGE;
import static com.app.config.RabbitConfig.RK_COMMENT_CREATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
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

        comment.setStatus(0);
        commentRepository.save(comment);

        Post post = postRepository.findById(comment.getPostId()).orElse(null);
        if (post != null && post.getCommentsCount() > 0) {
            post.setCommentsCount(post.getCommentsCount() - 1);
            postRepository.save(post);
        }

        log.info("Comment deleted: id={}, userId={}", commentId, userId);
    }

    @Override
    public PageResult<CommentVO> getPostComments(Long postId, Long currentUserId, Pageable pageable) {
        Page<Comment> page = commentRepository.findByPostIdAndStatusAndParentIdIsNull(postId, 1, pageable);
        List<CommentVO> vos = page.getContent().stream()
                .map(c -> buildCommentVO(c, currentUserId))
                .toList();
        return PageResult.of(vos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public PageResult<CommentVO> getCommentReplies(Long commentId, Long currentUserId, Pageable pageable) {
        List<Comment> replies = commentRepository.findByParentIdAndStatus(commentId, 1);
        List<CommentVO> vos = replies.stream()
                .map(c -> buildCommentVO(c, currentUserId))
                .toList();
        return PageResult.of(vos, 0, vos.size(), vos.size());
    }

    private CommentVO buildCommentVO(Comment comment, Long currentUserId) {
        UserVO author = userService.getUserById(comment.getUserId());
        UserVO replyToUser = comment.getReplyToUserId() != null
                ? userService.getUserById(comment.getReplyToUserId())
                : null;

        return CommentVO.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .author(author)
                .parentId(comment.getParentId())
                .replyToUser(replyToUser)
                .content(comment.getContent())
                .likesCount(comment.getLikesCount())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    public record CommentEvent(Long commentId, Long userId, Long postId) {}
}
