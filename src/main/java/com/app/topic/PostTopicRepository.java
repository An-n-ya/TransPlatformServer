package com.app.topic;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostTopicRepository extends JpaRepository<PostTopic, Long> {

    List<PostTopic> findByPostId(Long postId);

    List<PostTopic> findByPostIdIn(List<Long> postIds);

    void deleteByPostId(Long postId);

    void deleteByTopicId(Long topicId);

    long countByTopicId(Long topicId);

    List<PostTopic> findByTopicIdIn(List<Long> topicIds);

    /**
     * 重建话题帖数：按话题分组统计有效帖文数（用于 Redis 冷启动回填）
     * 返回 [topicId, postCount] 行数组
     */
    @Query(value = "SELECT pt.topic_id, COUNT(*) " +
            "FROM post_topics pt " +
            "JOIN posts p ON p.id = pt.post_id " +
            "WHERE p.status = 1 " +
            "GROUP BY pt.topic_id",
            nativeQuery = true)
    List<Object[]> countPostsByTopic();
}
