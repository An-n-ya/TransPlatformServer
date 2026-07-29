package com.app.content;

import com.app.common.PageResult;
import com.app.common.PostCreatedEvent;
import com.app.interaction.CollectionRepository;
import com.app.interaction.LikeRepository;
import com.app.user.UserService;
import com.app.user.UserVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.app.config.RabbitConfig.POST_EXCHANGE;
import static com.app.config.RabbitConfig.RK_POST_CREATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserService userService;
    private final LikeRepository likeRepository;
    private final CollectionRepository collectionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PostVO createPost(Long userId, PostCreateRequest request) {
        String imagesJson = null;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(request.getImages());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("图片列表序列化失败", e);
            }
        }

        Post post = new Post(userId, request.getContent(), imagesJson, request.getLocation());
        post = postRepository.save(post);

        // 保存图片元信息
        if (request.getImages() != null) {
            for (int i = 0; i < request.getImages().size(); i++) {
                postImageRepository.save(new PostImage(post.getId(), request.getImages().get(i), i));
            }
        }

        log.info("Post created: postId={}, userId={}", post.getId(), userId);

        // 异步推送 Feed 流和通知
        rabbitTemplate.convertAndSend(POST_EXCHANGE, RK_POST_CREATED, new PostCreatedEvent(post.getId(), userId));

        return buildPostVO(post, userId);
    }

    @Override
    @Cacheable(value = "post", key = "#postId", unless = "#result == null")
    public PostVO getPost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("帖文不存在"));

        if (post.getStatus() == 0) {
            throw new EntityNotFoundException("帖文已被删除");
        }

        return buildPostVO(post, currentUserId);
    }

    @Override
    @CacheEvict(value = "post", key = "#postId")
    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("帖文不存在"));

        if (!post.getUserId().equals(currentUserId)) {
            throw new SecurityException("无权删除他人的帖文");
        }

        post.setStatus(0);
        postRepository.save(post);
        log.info("Post deleted: postId={}, userId={}", postId, currentUserId);
    }

    @Override
    public PageResult<PostVO> getUserPosts(Long userId, Long currentUserId, Pageable pageable) {
        Page<Post> page = postRepository.findByUserIdAndStatus(userId, 1, pageable);
        List<PostVO> vos = page.getContent().stream()
                .map(post -> buildPostVO(post, currentUserId))
                .toList();
        return PageResult.of(vos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public List<PostVO> getPostsByIds(List<Long> ids, Long currentUserId) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Post> posts = postRepository.findByIdInAndStatus(ids, 1);

        // 按 ids 顺序排序
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));
        return ids.stream()
                .map(postMap::get)
                .filter(p -> p != null)
                .map(post -> buildPostVO(post, currentUserId))
                .toList();
    }

    /** 构建帖文 VO */
    private PostVO buildPostVO(Post post, Long currentUserId) {
        UserVO author = userService.getUserById(post.getUserId());

        List<String> images = parseImages(post.getImages());

        Boolean liked = null;
        Boolean collected = null;
        if (currentUserId != null) {
            liked = likeRepository.existsByUserIdAndTargetTypeAndTargetId(
                    currentUserId, "post", post.getId());
            collected = collectionRepository.existsByUserIdAndPostId(
                    currentUserId, post.getId());
        }

        return PostVO.builder()
                .id(post.getId())
                .author(author)
                .content(post.getContent())
                .images(images)
                .location(post.getLocation())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .collectionsCount(post.getCollectionsCount())
                .liked(liked)
                .collected(collected)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse images JSON: {}", imagesJson, e);
            return Collections.emptyList();
        }
    }
}
