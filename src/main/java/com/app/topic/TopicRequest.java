package com.app.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "话题创建/更新请求")
public class TopicRequest {

    @NotBlank(message = "话题名称不能为空")
    @Size(max = 50, message = "话题名称最长 50 个字符")
    @Schema(description = "话题名称", example = "旅行")
    private String name;

    @Size(max = 200, message = "话题描述最长 200 个字符")
    @Schema(description = "话题描述")
    private String description;
}
