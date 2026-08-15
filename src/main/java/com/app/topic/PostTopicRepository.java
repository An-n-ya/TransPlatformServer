package com.app.topic;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
