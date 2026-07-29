package com.app.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByUserIdAndStatus(Long userId, Integer status, Pageable pageable);

    Page<Post> findByStatus(Integer status, Pageable pageable);

    List<Post> findByIdInAndStatus(List<Long> ids, Integer status);
}
