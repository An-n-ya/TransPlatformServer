package com.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户资料更新请求")
public class UserUpdateRequest {

    @Size(max = 100, message = "昵称最长 100 个字符")
    @Schema(description = "昵称")
    private String nickname;

    @Size(max = 500, message = "头像 URL 最长 500 个字符")
    @Schema(description = "头像 URL")
    private String avatar;

    @Size(max = 200, message = "个人简介最长 200 个字符")
    @Schema(description = "个人简介")
    private String bio;

    @Size(max = 500, message = "背景图 URL 最长 500 个字符")
    @Schema(description = "个人主页背景图 URL")
    private String bioHeaderImg;
}
