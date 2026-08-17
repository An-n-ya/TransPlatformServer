package com.app.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 帖文统一查询请求（JSON body 传递）
 *
 * 查询优先级：
 *  1. postId 非空  → 按帖文 ID 查询单篇
 *  2. content 非空 → 按内容模糊匹配（必须提供 userId）
 *  3. topicId 非空 → 按话题查询（可选叠加 userId 限定）
 *  4. userId 非空  → 该用户全部帖文
 *  5. 全部为空     → 报错提示（必须提供至少一个查询参数）
 */
@Data
@Schema(description = "帖文统一查询请求")
public class PostQueryRequest {

    @Schema(description = "帖文ID（可选，查询单篇帖文）")
    private Long postId;

    @Schema(description = "用户ID（可选，按用户查询；提供 content 时必填）")
    private Long userId;

    @Schema(description = "话题ID（可选，按话题查询）")
    private Long topicId;

    @Schema(description = "内容关键词（可选，按内容模糊匹配；提供时必须同时提供 userId）")
    private String content;
}
