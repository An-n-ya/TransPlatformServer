package com.app.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * 按用户名或昵称模糊搜索（仅正常用户）
     */
    @Query("SELECT u FROM User u WHERE u.status = 1 " +
            "AND (u.username LIKE '%' || :keyword || '%' " +
            "     OR u.nickname LIKE '%' || :keyword || '%')")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
