package com.app.invitation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "发送邀请码邮件请求")
public class SendInvitationEmailRequest {

    @NotBlank(message = "收件人邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "收件人邮箱", example = "friend@example.com")
    private String email;

    @Min(value = 1, message = "有效期最少 1 天")
    @Max(value = 365, message = "有效期最多 365 天")
    @Schema(description = "有效期（天）", example = "7")
    private Integer days = 7;

    @Schema(description = "场景标识", example = "default")
    private String scene = "default";
}
