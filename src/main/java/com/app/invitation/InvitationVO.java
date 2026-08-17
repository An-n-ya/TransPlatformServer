package com.app.invitation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邀请码视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邀请码视图")
public class InvitationVO {

    private Long id;
    private String code;
    private Long inviterId;
    private Long inviteeId;
    private Integer status;
    private LocalDateTime expiredAt;
    private String scene;
    private LocalDateTime createdAt;
}
