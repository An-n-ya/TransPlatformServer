package com.app.interaction;

import com.app.content.Post;
import com.app.content.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.app.config.RabbitConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    @CacheEvict(value = "post", key = "#targetId")
    public void like(Long userId, String targetType, Long targetId) {
        if (likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)) {
            throw new IllegalStateException("已经点过赞了");
        }

        likeRepository.save(new Like(userId, targetType, targetId));

        // 更新帖文点赞计数
        if ("post".equals(targetType)) {
            Post post = postRepository.findById(targetId)
                    .orElseThrow(() -> new EntityNotFoundException("帖文不存在"));
            post.setLikesCount(post.getLikesCount() + 1);
            postRepository.save(post);
        }

        rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, RK_LIKE_CREATED,
                new LikeEvent(userId, targetType, targetId));
        log.info("Like created: userId={}, targetType={}, targetId={}", userId, targetType, targetId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "post", key = "#targetId")
    public void unlike(Long userId, String targetType, Long targetId) {
        Like like = likeRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .orElseThrow(() -> new IllegalStateException("未点赞"));

        likeRepository.delete(like);

        if ("post".equals(targetType)) {
            Post post = postRepository.findById(targetId)
                    .orElseThrow(() -> new EntityNotFoundException("帖文不存在"));
            if (post.getLikesCount() > 0) {
                post.setLikesCount(post.getLikesCount() - 1);
                postRepository.save(post);
            }
        }

        log.info("Like removed: userId={}, targetType={}, targetId={}", userId, targetType, targetId);
    }

    /** 点赞事件（用于 RabbitMQ 消息） */
    public record LikeEvent(Long userId, String targetType, Long targetId) {}
}
