package com.app.topic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    boolean existsByName(String name);

    Optional<Topic> findByName(String name);

    Page<Topic> findByStatus(Integer status, Pageable pageable);

    Page<Topic> findByNameContainingIgnoreCaseAndStatus(String keyword, Integer status, Pageable pageable);

    boolean existsByIdAndStatus(Long id, Integer status);
}
