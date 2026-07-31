package com.app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "设置置顶帖请求")
public class SetPinnedPostRequest {

    @NotNull(message = "帖文ID不能为空")
    @Schema(description = "要置顶的帖文ID")
    private Long postId;
}
