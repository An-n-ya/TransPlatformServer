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
     * 热门话题：按参与人数（去重用户数）降序
     * 返回 [topicId, participantCount] 行数组
     */
    @Query(value = "SELECT pt.topic_id, COUNT(DISTINCT p.user_id) " +
            "FROM post_topics pt " +
            "JOIN posts p ON p.id = pt.post_id " +
            "WHERE p.status = 1 " +
            "GROUP BY pt.topic_id " +
            "ORDER BY COUNT(DISTINCT p.user_id) DESC",
            nativeQuery = true)
    List<Object[]> findHotTopicsByParticipants(Pageable pageable);
}
