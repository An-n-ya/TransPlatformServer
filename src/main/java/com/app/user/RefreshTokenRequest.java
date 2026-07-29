package com.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "刷新 Token 请求")
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken 不能为空")
    @Schema(description = "刷新令牌")
    private String refreshToken;
}
