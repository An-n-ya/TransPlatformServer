package com.app.invitation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "生成邀请码请求")
public class GenerateInvitationRequest {

    @Min(value = 1, message = "数量最小 1")
    @Max(value = 100, message = "数量最大 100")
    @Schema(description = "生成数量", example = "1")
    private Integer count = 1;

    @Min(value = 1, message = "有效期最少 1 天")
    @Max(value = 365, message = "有效期最多 365 天")
    @Schema(description = "有效期（天）", example = "7")
    private Integer days = 7;

    @Schema(description = "场景标识", example = "default")
    private String scene = "default";
}
