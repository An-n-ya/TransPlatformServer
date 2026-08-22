package com.app.topic;

import com.app.common.PageResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    /** 话题帖数排名 ZSET：member=topicId, score=有效帖文数 */
    private static final String HOT_TOPIC_KEY = "topic:hot";

    private final TopicRepository topicRepository;
    private final PostTopicRepository postTopicRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public TopicVO createTopic(TopicRequest request) {
        if (topicRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("话题已存在: " + request.getName());
        }
        Topic topic = topicRepository.save(new Topic(request.getName(), request.getDescription()));
        log.info("Topic created: id={}, name={}", topic.getId(), topic.getName());
        return TopicVO.from(topic, 0L);
    }

    @Override
    @Transactional
    public TopicVO updateTopic(Long topicId, TopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("话题不存在"));
        if (topic.getStatus() == 0) {
            throw new EntityNotFoundException("话题已被删除");
        }

        if (!topic.getName().equals(request.getName())
                && topicRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("话题已存在: " + request.getName());
        }

        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic = topicRepository.save(topic);

        log.info("Topic updated: id={}", topicId);
        return TopicVO.from(topic, getPostCount(topicId));
    }

    @Override
    @Transactional
    public void deleteTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("话题不存在"));
        if (topic.getStatus() == 0) {
            throw new EntityNotFoundException("话题已被删除");
        }

        // 逻辑删除：仅置 status=0，保留数据用于审计/恢复
        topic.setStatus(0);
        topicRepository.save(topic);

        // 清理帖文关联与 Redis 计数
        postTopicRepository.deleteByTopicId(topicId);
        stringRedisTemplate.opsForZSet().remove(HOT_TOPIC_KEY, topicId.toString());
        log.info("Topic deleted (logical): id={}, name={}", topicId, topic.getName());
    }

    @Override
    public TopicVO getTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("话题不存在"));
        if (topic.getStatus() == 0) {
            throw new EntityNotFoundException("话题已被删除");
        }
        return TopicVO.from(topic, getPostCount(topicId));
    }

    @Override
    public PageResult<TopicVO> listTopics(Pageable pageable) {
        Page<Topic> page = topicRepository.findByStatus(1, pageable);
        List<TopicVO> vos = page.getContent().stream()
                .map(t -> TopicVO.from(t, getPostCount(t.getId())))
                .toList();
        return PageResult.of(vos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public PageResult<TopicVO> searchTopics(String keyword, Pageable pageable) {
        Page<Topic> page = topicRepository.findByNameContainingIgnoreCaseAndStatus(keyword, 1, pageable);
        List<TopicVO> vos = page.getContent().stream()
                .map(t -> TopicVO.from(t, getPostCount(t.getId())))
                .toList();
        return PageResult.of(vos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public List<TopicVO> getHotTopics(int limit) {
        // Redis ZSET 直接按帖数取前 N
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(HOT_TOPIC_KEY, 0, limit - 1);

        // Redis 冷启动（ZSET 为空）时从数据库重建
        if (tuples == null || tuples.isEmpty()) {
            rebuildHotTopics();
            tuples = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(HOT_TOPIC_KEY, 0, limit - 1);
        }
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        return tuples.stream()
                .map(t -> {
                    Long topicId = Long.parseLong(t.getValue());
                    Topic topic = topicRepository.findById(topicId).orElse(null);
                    // 过滤已逻辑删除的话题
                    if (topic == null || topic.getStatus() == 0) return null;
                    return topic;
                })
                .filter(Objects::nonNull)
                .map(t -> TopicVO.from(t, getPostCount(t.getId())))
                .toList();
    }

    // =====================================================================
    // Redis 计数（完成 TODO：帖数缓存在 Redis，发帖时自增，
    // 只有 Redis 未命中时才查数据库，查完后回填 Redis）
    // =====================================================================

    /** 帖文发布时调用：话题帖数 +1 */
    @Transactional
    public void incrementPostCount(Long topicId) {
        stringRedisTemplate.opsForZSet().incrementScore(HOT_TOPIC_KEY, topicId.toString(), 1);
    }

    /** 帖文删除时调用：话题帖数 -1，归零则移除 */
    @Transactional
    public void decrementPostCount(Long topicId) {
        Double score = stringRedisTemplate.opsForZSet()
                .incrementScore(HOT_TOPIC_KEY, topicId.toString(), -1);
        if (score != null && score <= 0) {
            stringRedisTemplate.opsForZSet().remove(HOT_TOPIC_KEY, topicId.toString());
        }
    }

    /**
     * 获取话题帖数：Redis 优先（ZSCORE），
     * 未命中 → 查数据库 → 回填 Redis
     */
    private Long getPostCount(Long topicId) {
        Double score = stringRedisTemplate.opsForZSet()
                .score(HOT_TOPIC_KEY, topicId.toString());
        if (score != null) {
            return score.longValue();
        }
        Long count = postTopicRepository.countByTopicId(topicId);
        stringRedisTemplate.opsForZSet().add(HOT_TOPIC_KEY, topicId.toString(), count);
        return count;
    }

    /** Redis 冷启动：从数据库重建全部话题帖数 */
    private void rebuildHotTopics() {
        List<Object[]> rows = postTopicRepository.countPostsByTopic();
        for (Object[] row : rows) {
            Long topicId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            stringRedisTemplate.opsForZSet().add(HOT_TOPIC_KEY, topicId.toString(), count);
        }
        log.debug("Hot topics rebuilt from DB: {} topics", rows.size());
    }
}
