package com.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户视图对象（公开信息，不包含密码等敏感字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户公开信息")
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private String bioHeaderImg;
    private Long pinnedPostId;
    private Integer status;
    private Long followersCount;
    private Long followeesCount;
    private LocalDateTime createdAt;

    public static UserVO from(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .bioHeaderImg(user.getBioHeaderImg())
                .pinnedPostId(user.getPinnedPostId())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
