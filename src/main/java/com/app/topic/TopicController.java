package com.app.topic;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * 话题控制器 — CRUD
 */
@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
@Tag(name = "话题", description = "话题的创建、查询、更新、删除")
public class TopicController {

    private final TopicService topicService;

    @PostMapping
    @Operation(summary = "创建话题")
    public ApiResponse<TopicVO> createTopic(@Valid @RequestBody TopicRequest request) {
        return ApiResponse.success(topicService.createTopic(request));
    }

    @GetMapping
    @Operation(summary = "话题列表（分页）")
    public ApiResponse<PageResult<TopicVO>> listTopics(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(topicService.listTopics(pageable));
    }

    @GetMapping("/{topicId}")
    @Operation(summary = "获取话题详情")
    public ApiResponse<TopicVO> getTopic(@PathVariable Long topicId) {
        return ApiResponse.success(topicService.getTopic(topicId));
    }

    @PutMapping("/{topicId}")
    @Operation(summary = "更新话题")
    public ApiResponse<TopicVO> updateTopic(@PathVariable Long topicId,
                                            @Valid @RequestBody TopicRequest request) {
        return ApiResponse.success(topicService.updateTopic(topicId, request));
    }

    @DeleteMapping("/{topicId}")
    @Operation(summary = "删除话题")
    public ApiResponse<Void> deleteTopic(@PathVariable Long topicId) {
        topicService.deleteTopic(topicId);
        return ApiResponse.success();
    }
}
