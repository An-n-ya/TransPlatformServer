package com.app.interaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发表评论请求")
public class CommentCreateRequest {

    @Schema(description = "帖文ID（由路径参数传入，不参与请求体校验）")
    private Long postId;

    @Schema(description = "父评论ID（回复时传入）")
    private Long parentId;

    @Schema(description = "回复的目标用户ID")
    private Long replyToUserId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容最长 1000 个字符")
    @Schema(description = "评论内容")
    private String content;
}
