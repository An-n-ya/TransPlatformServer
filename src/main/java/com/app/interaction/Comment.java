package com.app.interaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 评论实体（支持嵌套回复）
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "reply_to_user_id")
    private Long replyToUserId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Comment(Long postId, Long userId, Long parentId, Long replyToUserId, String content) {
        this.postId = postId;
        this.userId = userId;
        this.parentId = parentId;
        this.replyToUserId = replyToUserId;
        this.content = content;
        this.status = 1;
    }
}
