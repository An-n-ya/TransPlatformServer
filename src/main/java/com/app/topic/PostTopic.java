package com.app.topic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 帖文-话题关联实体（多对多）
 */
@Entity
@Table(name = "post_topics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id", "topic_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class PostTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public PostTopic(Long postId, Long topicId) {
        this.postId = postId;
        this.topicId = topicId;
    }
}
