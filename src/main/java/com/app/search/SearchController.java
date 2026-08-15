package com.app.search;

import com.app.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索控制器 — 按类别搜索（可扩展：user / topic / ...）
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "搜索", description = "按类别搜索，目前支持 user 类别（可扩展 topic 等）")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "按类别搜索")
    public ApiResponse<SearchResult<?>> search(
            @RequestParam @NotBlank(message = "类别不能为空")
            @Pattern(regexp = "^[a-z0-9_]+$", message = "类别仅支持小写字母、数字、下划线")
            String category,

            @RequestParam @NotBlank(message = "关键词不能为空")
            @Size(max = 100, message = "关键词最长 100 个字符")
            String keyword,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(searchService.search(category, keyword, pageable));
    }
}
