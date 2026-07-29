package com.app.feed;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import com.app.content.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Feed 流控制器
 */
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Tag(name = "Feed 流", description = "首页 Feed 流（基于关注关系的时间线）")
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    @Operation(summary = "获取首页 Feed 流（分页）")
    public ApiResponse<PageResult<PostVO>> getFeed(@AuthenticationPrincipal Long userId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(feedService.getFeed(userId, page, size));
    }
}
