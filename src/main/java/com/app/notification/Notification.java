package com.app.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 通知实体
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_user_unread", columnList = "user_id, is_read"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(length = 200)
    private String title;

    @Column(length = 500)
    private String content;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "is_read", nullable = false)
    private Integer isRead = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Notification(Long userId, String type, String title, String content, Long fromUserId, Long targetId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.fromUserId = fromUserId;
        this.targetId = targetId;
        this.isRead = 0;
    }
}
