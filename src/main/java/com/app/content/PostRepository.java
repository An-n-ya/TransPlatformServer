package com.app.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByUserIdAndStatus(Long userId, Integer status, Pageable pageable);

    Page<Post> findByStatus(Integer status, Pageable pageable);

    List<Post> findByIdInAndStatus(List<Long> ids, Integer status);

    /**
     * 按内容模糊匹配某用户的帖文
     */
    @Query("SELECT p FROM Post p WHERE p.userId = :userId AND p.status = 1 " +
            "AND p.content LIKE '%' || :content || '%'")
    Page<Post> findByUserIdAndContentContaining(@Param("userId") Long userId,
                                                @Param("content") String content,
                                                Pageable pageable);

    /**
     * 按话题查询某用户的帖文
     */
    @Query("SELECT p FROM Post p WHERE p.userId = :userId AND p.status = 1 " +
            "AND p.id IN (SELECT pt.postId FROM PostTopic pt WHERE pt.topicId = :topicId)")
    Page<Post> findByUserIdAndTopicId(@Param("userId") Long userId,
                                      @Param("topicId") Long topicId,
                                      Pageable pageable);

    /**
     * 按话题查询全部用户的帖文
     */
    @Query("SELECT p FROM Post p WHERE p.status = 1 " +
            "AND p.id IN (SELECT pt.postId FROM PostTopic pt WHERE pt.topicId = :topicId)")
    Page<Post> findByTopicId(@Param("topicId") Long topicId, Pageable pageable);
}
