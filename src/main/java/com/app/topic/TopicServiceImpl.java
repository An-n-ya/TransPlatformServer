package com.app.topic;

import com.app.common.PageResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final PostTopicRepository postTopicRepository;

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

        if (!topic.getName().equals(request.getName())
                && topicRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("话题已存在: " + request.getName());
        }

        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic = topicRepository.save(topic);

        // TODO: 直接从数据库获取topicCount太消耗数据库，这个数据可以缓存到redis。当新增贴文时，增加对应的count计数，只有在redis中查询不到的时候，才考虑在数据库中查找,并在查找成功后，重新载入redis
        long postCount = postTopicRepository.countByTopicId(topicId);
        log.info("Topic updated: id={}", topicId);
        return TopicVO.from(topic, postCount);
    }

    @Override
    @Transactional
    public void deleteTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("话题不存在"));

        // 清理帖文关联
        postTopicRepository.deleteByTopicId(topicId);
        topicRepository.delete(topic);
        log.info("Topic deleted: id={}, name={}", topicId, topic.getName());
    }

    @Override
    public TopicVO getTopic(Long topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("话题不存在"));
        return TopicVO.from(topic, postTopicRepository.countByTopicId(topicId));
    }

    @Override
    public PageResult<TopicVO> listTopics(Pageable pageable) {
        Page<Topic> page = topicRepository.findAll(pageable);
        List<TopicVO> vos = attachPostCount(page.getContent());
        return PageResult.of(vos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public PageResult<TopicVO> searchTopics(String keyword, Pageable pageable) {
        Page<Topic> page = topicRepository.findByNameContainingIgnoreCase(keyword, pageable);
        List<TopicVO> vos = attachPostCount(page.getContent());
        return PageResult.of(vos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public List<TopicVO> getHotTopics(int limit) {
        List<Object[]> rows = postTopicRepository
                .findHotTopicsByParticipants(PageRequest.of(0, limit));
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> topicIds = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .toList();
        Map<Long, Long> participantMap = rows.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()));

        // 批量查帖数，保持排序
        List<PostTopic> links = postTopicRepository.findByTopicIdIn(topicIds);
        Map<Long, Long> postCountMap = links.stream()
                .collect(Collectors.groupingBy(PostTopic::getTopicId, Collectors.counting()));

        return topicIds.stream()
                .map(id -> topicRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(topic -> {
                    TopicVO vo = TopicVO.from(topic, postCountMap.getOrDefault(topic.getId(), 0L));
                    vo.setParticipantCount(participantMap.getOrDefault(topic.getId(), 0L));
                    return vo;
                })
                .toList();
    }

    /** 批量统计话题下帖文数 */
    private List<TopicVO> attachPostCount(List<Topic> topics) {
        if (topics.isEmpty()) {
            return List.of();
        }
        List<Long> topicIds = topics.stream().map(Topic::getId).toList();
        List<PostTopic> links = postTopicRepository.findByTopicIdIn(topicIds);
        Map<Long, Long> countMap = links.stream()
                .collect(Collectors.groupingBy(PostTopic::getTopicId, Collectors.counting()));

        return topics.stream()
                .map(t -> TopicVO.from(t, countMap.getOrDefault(t.getId(), 0L)))
                .toList();
    }
}
