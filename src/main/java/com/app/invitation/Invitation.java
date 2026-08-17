package com.app.invitation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 邀请码实体
 */
@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
public class Invitation {

    /** 状态：0=有效 1=已使用 2=已过期 */
    public static final int STATUS_VALID = 0;
    public static final int STATUS_USED = 1;
    public static final int STATUS_EXPIRED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(name = "inviter_id", nullable = false)
    private Long inviterId;

    @Column(name = "invitee_id")
    private Long inviteeId;

    @Column(nullable = false)
    private Integer status = STATUS_VALID;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false, length = 30)
    private String scene = "default";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Invitation(String code, Long inviterId, LocalDateTime expiredAt, String scene) {
        this.code = code;
        this.inviterId = inviterId;
        this.expiredAt = expiredAt;
        this.scene = scene != null ? scene : "default";
        this.status = STATUS_VALID;
    }
}
