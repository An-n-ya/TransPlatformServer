package com.app.content;

import com.app.common.PageResult;
import com.app.common.PostCreatedEvent;
import com.app.interaction.CollectionRepository;
import com.app.interaction.LikeRepository;
import com.app.upload.ImageValidator;
import com.app.upload.StorageService;
import com.app.upload.UploadRequest;
import com.app.upload.UploadResult;
import com.app.topic.PostTopic;
import com.app.topic.PostTopicRepository;
import com.app.topic.Topic;
import com.app.topic.TopicRepository;
import com.app.topic.TopicService;
import com.app.topic.TopicVO;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final TopicRepository topicRepository;
    private final PostTopicRepository postTopicRepository;
    private final TopicService topicService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final ImageValidator imageValidator;

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

        // 关联话题
        linkTopics(post.getId(), request.getTopicIds());

        log.info("Post created: postId={}, userId={}", post.getId(), userId);

        // 异步推送 Feed 流和通知
        rabbitTemplate.convertAndSend(POST_EXCHANGE, RK_POST_CREATED, new PostCreatedEvent(post.getId(), userId));

        return buildPostVO(post, userId);
    }

    @Override
    @Transactional
    public PostVO createPost(Long userId, String content, String location, List<Long> topicIds, List<MultipartFile> images) {
        List<String> imageUrls = Collections.emptyList();
        if (images != null && !images.isEmpty()) {
            imageValidator.validate(images);
            imageUrls = images.stream().map(file -> {
                try {
                    UploadRequest req = new UploadRequest(
                            file.getInputStream(),
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getSize(),
                            "posts");
                    UploadResult result = storageService.upload(req);
                    return result.url();
                } catch (IOException e) {
                    throw new RuntimeException("图片上传失败: " + file.getOriginalFilename(), e);
                }
            }).toList();
        }

        String imagesJson = null;
        if (!imageUrls.isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(imageUrls);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("图片列表序列化失败", e);
            }
        }

        Post post = new Post(userId, content, imagesJson, location);
        post = postRepository.save(post);

        if (!imageUrls.isEmpty()) {
            for (int i = 0; i < imageUrls.size(); i++) {
                postImageRepository.save(new PostImage(post.getId(), imageUrls.get(i), i));
            }
        }

        // 关联话题
        linkTopics(post.getId(), topicIds);

        log.info("Post created (multipart): postId={}, userId={}, images={}", post.getId(), userId, imageUrls.size());

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

        doDeletePost(post);
    }

    @Override
    @CacheEvict(value = "post", key = "#postId")
    @Transactional
    public void deletePostByAdmin(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("帖文不存在"));

        doDeletePost(post);
        log.info("Post deleted by admin: postId={}", postId);
    }

    /** 帖文逻辑删除（置 status=0，并递减关联话题计数） */
    private void doDeletePost(Post post) {
        if (post.getStatus() == 0) {
            throw new EntityNotFoundException("帖文已被删除");
        }

        // 删除前记录关联话题，用于递减 Redis 计数
        List<Long> topicIds = postTopicRepository.findByPostId(post.getId()).stream()
                .map(PostTopic::getTopicId)
                .toList();

        post.setStatus(0);
        postRepository.save(post);

        // 递减话题帖数计数
        for (Long topicId : topicIds) {
            topicService.decrementPostCount(topicId);
        }

        log.info("Post deleted: postId={}", post.getId());
    }

    @Override
    public PageResult<PostVO> adminListPosts(Long userId, String content, Integer status, Pageable pageable) {
        String keyword = (content == null || content.isBlank()) ? null : content.trim();
        Page<Post> page = postRepository.adminSearch(userId, keyword, status, pageable);
        List<PostVO> vos = page.getContent().stream()
                .map(post -> buildPostVO(post, null))
                .toList();
        return PageResult.of(vos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public PageResult<PostVO> queryPosts(PostQueryRequest query, Long currentUserId, Pageable pageable) {
        // 1. 按 postId 查询单篇
        if (query != null && query.getPostId() != null) {
            Post post = postRepository.findById(query.getPostId())
                    .filter(p -> p.getStatus() == 1)
                    .orElseThrow(() -> new EntityNotFoundException("帖文不存在"));
            return PageResult.of(List.of(buildPostVO(post, currentUserId)), 0, pageable.getPageSize(), 1);
        }

        Page<Post> page;

        // 2. 按内容模糊匹配（必须提供 userId）
        if (query != null && query.getContent() != null && !query.getContent().isBlank()) {
            if (query.getUserId() == null) {
                throw new IllegalArgumentException("按内容查询时必须提供 userId");
            }
            page = postRepository.findByUserIdAndContentContaining(
                    query.getUserId(), query.getContent().trim(), pageable);
        }
        // 3. 按话题查询（可选叠加 userId）
        else if (query != null && query.getTopicId() != null) {
            page = query.getUserId() != null
                    ? postRepository.findByUserIdAndTopicId(query.getUserId(), query.getTopicId(), pageable)
                    : postRepository.findByTopicId(query.getTopicId(), pageable);
        }
        // 4. 按用户查询
        else if (query != null && query.getUserId() != null) {
            page = postRepository.findByUserIdAndStatus(query.getUserId(), 1, pageable);
        }
        // 5. 无条件：未提供任何查询参数，报错提示
        else {
            throw new IllegalArgumentException("请提供至少一个查询参数：postId / userId / topicId / content");
        }

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
                .topics(getTopicsByPostId(post.getId()))
                .liked(liked)
                .collected(collected)
                .createdAt(post.getCreatedAt())
                .build();
    }

    /** 关联帖文与话题 */
    private void linkTopics(Long postId, List<Long> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return;
        }
        for (Long topicId : topicIds) {
            if (!topicRepository.existsByIdAndStatus(topicId, 1)) {
                throw new EntityNotFoundException("话题不存在: id=" + topicId);
            }
            postTopicRepository.save(new PostTopic(postId, topicId));
            // Redis 帖数计数 +1
            topicService.incrementPostCount(topicId);
        }
    }

    /** 查询帖文关联的话题 */
    private List<TopicVO> getTopicsByPostId(Long postId) {
        List<PostTopic> links = postTopicRepository.findByPostId(postId);
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        return links.stream()
                .map(link -> {
                    Topic topic = topicRepository.findById(link.getTopicId()).orElse(null);
                    // 过滤已逻辑删除的话题
                    if (topic == null || topic.getStatus() == 0) return null;
                    return TopicVO.from(topic, postTopicRepository.countByTopicId(topic.getId()));
                })
                .filter(java.util.Objects::nonNull)
                .toList();
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
