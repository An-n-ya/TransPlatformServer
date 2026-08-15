package com.app.search;

import com.app.common.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索结果响应 — 包含类别、关键词与分页内容
 * @param <T> 内容元素类型（如 UserVO）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "搜索结果")
public class SearchResult<T> {

    @Schema(description = "搜索类别", example = "user")
    private String category;

    @Schema(description = "搜索关键词", example = "alice")
    private String keyword;

    @Schema(description = "分页结果")
    private PageResult<T> data;
}
