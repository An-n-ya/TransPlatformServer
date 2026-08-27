package com.app.feed;

import com.app.common.ApiResponse;
import com.app.common.CursorPage;
import com.app.content.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Feed 流控制器
 */
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Validated
@Tag(name = "Feed 流", description = "首页 Feed 流：plaza 广场(默认)/following 关注/nearby 附近，游标分页")
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    @Operation(summary = "获取首页 Feed 流（游标分页）",
            description = "type：plaza 广场(默认，所有用户最新帖文) / following 关注(关注用户的最新帖文) / nearby 附近(按用户位置过滤)。第一页不传 cursor；之后将上一页返回的 nextCursor 原样传入即可")
    public ApiResponse<CursorPage<PostVO>> getFeed(@AuthenticationPrincipal Long userId,
                                                   @RequestParam(defaultValue = "plaza") String type,
                                                   @RequestParam(required = false) Long cursor,
                                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(feedService.getFeed(userId, FeedType.from(type), cursor, size));
    }
}
