package com.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "重置密码请求")
public class ResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "已注册邮箱", example = "user@example.com")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码为 6 位数字")
    @Schema(description = "验证码", example = "123456")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度 6-100 个字符")
    @Schema(description = "新密码", example = "newpassword123")
    private String newPassword;
}
