package com.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度 3-50 个字符")
    @Schema(description = "用户名", example = "alice")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 100, message = "昵称最长 100 个字符")
    @Schema(description = "昵称", example = "Alice")
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度 6-100 个字符")
    @Schema(description = "密码", example = "password123")
    private String password;
}
