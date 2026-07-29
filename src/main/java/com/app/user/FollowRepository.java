package com.app.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    boolean existsByFollowerIdAndFolloweeIdAndStatus(Long followerId, Long followeeId, Integer status);

    /** 获取粉丝列表（关注我的人） */
    Page<Follow> findByFolloweeIdAndStatus(Long followeeId, Integer status, Pageable pageable);

    /** 获取关注列表（我关注的人） */
    Page<Follow> findByFollowerIdAndStatus(Long followerId, Integer status, Pageable pageable);

    /** 获取所有粉丝ID */
    @Query("SELECT f.followerId FROM Follow f WHERE f.followeeId = :userId AND f.status = 1")
    List<Long> findFollowerIdsByFolloweeId(Long userId);

    /** 获取所有关注者ID */
    @Query("SELECT f.followeeId FROM Follow f WHERE f.followerId = :userId AND f.status = 1")
    List<Long> findFolloweeIdsByFollowerId(Long userId);

    @Modifying
    @Query("UPDATE Follow f SET f.status = :status WHERE f.followerId = :followerId AND f.followeeId = :followeeId")
    int updateStatus(Long followerId, Long followeeId, Integer status);

    long countByFolloweeIdAndStatus(Long followeeId, Integer status);

    long countByFollowerIdAndStatus(Long followerId, Integer status);
}
