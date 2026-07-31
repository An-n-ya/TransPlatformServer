package com.app.interaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdAndStatusAndParentIdIsNull(Long postId, Integer status, Pageable pageable);

    List<Comment> findByParentIdAndStatus(Long parentId, Integer status);

    Page<Comment> findByPostIdAndStatus(Long postId, Integer status, Pageable pageable);

    long countByPostIdAndStatus(Long postId, Integer status);

    long countByParentIdAndStatus(Long parentId, Integer status);

    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, Integer status);
}
