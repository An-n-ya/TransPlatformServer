package com.app.admin;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import com.app.topic.TopicRequest;
import com.app.topic.TopicService;
import com.app.topic.TopicVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台话题控制器 — 话题完整 CRUD（删除使用逻辑删除）
 */
@RestController
@RequestMapping("/admin/v1/topics")
@RequiredArgsConstructor
@Tag(name = "管理后台-话题", description = "管理员话题完整 CRUD（删除为逻辑删除）")
public class AdminTopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "管理员话题列表（分页）")
    public ApiResponse<PageResult<TopicVO>> listTopics(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(topicService.listTopics(pageable));
    }

    @PostMapping
    @Operation(summary = "管理员创建话题")
    public ApiResponse<TopicVO> createTopic(@Valid @RequestBody TopicRequest request) {
        return ApiResponse.success(topicService.createTopic(request));
    }

    @GetMapping("/hot")
    @Operation(summary = "管理员热门话题（按帖数最多的前 N 个）")
    public ApiResponse<List<TopicVO>> getHostTopics(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(topicService.getHotTopics(Math.min(limit, 50)));
    }

    @GetMapping("/{topicId}")
    @Operation(summary = "管理员获取话题详情")
    public ApiResponse<TopicVO> getTopic(@PathVariable Long topicId) {
        return ApiResponse.success(topicService.getTopic(topicId));
    }

    @PutMapping("/{topicId}")
    @Operation(summary = "管理员更新话题")
    public ApiResponse<TopicVO> updateTopic(@PathVariable Long topicId,
                                            @Valid @RequestBody TopicRequest request) {
        return ApiResponse.success(topicService.updateTopic(topicId, request));
    }

    @DeleteMapping("/{topicId}")
    @Operation(summary = "管理员删除话题（逻辑删除）")
    public ApiResponse<Void> deleteTopic(@PathVariable Long topicId) {
        topicService.deleteTopic(topicId);
        return ApiResponse.success();
    }
}
