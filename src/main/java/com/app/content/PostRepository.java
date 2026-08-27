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
     * 广场时间流：所有用户的正常帖文，按 id 倒序（最新在前）。
     * cursor 为 null 时从最新开始；否则取 id 小于 cursor 的帖子。
     */
    @Query("SELECT p FROM Post p WHERE p.status = 1 " +
            "AND (:cursor IS NULL OR p.id < :cursor) ORDER BY p.id DESC")
    List<Post> findPlazaFeed(@Param("cursor") Long cursor, Pageable pageable);

    /**
     * 附近时间流：位置等于给定 location 的用户的正常帖文，按 id 倒序（最新在前）。
     * cursor 为 null 时从最新开始；否则取 id 小于 cursor 的帖子。
     */
    @Query("SELECT p FROM Post p WHERE p.status = 1 " +
            "AND p.userId IN (SELECT u.id FROM User u WHERE u.location = :location) " +
            "AND (:cursor IS NULL OR p.id < :cursor) ORDER BY p.id DESC")
    List<Post> findNearbyFeed(@Param("location") String location,
                              @Param("cursor") Long cursor,
                              Pageable pageable);

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

    /**
     * 管理员查询帖文列表（userId / content / status 均可为空）
     */
    @Query("SELECT p FROM Post p WHERE " +
            "(:status IS NULL OR p.status = :status) " +
            "AND (:userId IS NULL OR p.userId = :userId) " +
            "AND (:content IS NULL OR p.content LIKE '%' || :content || '%')")
    Page<Post> adminSearch(@Param("userId") Long userId,
                           @Param("content") String content,
                           @Param("status") Integer status,
                           Pageable pageable);
}
