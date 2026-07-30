package com.app.notification;

import com.app.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知视图对象")
public class NotificationVO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private UserVO fromUser;
    private Long targetId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationVO from(Notification n, UserVO fromUser) {
        return NotificationVO.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .fromUser(fromUser)
                .targetId(n.getTargetId())
                .isRead(n.getIsRead() == 1)
                .createdAt(n.getCreatedAt())
                .build();
    }
}
