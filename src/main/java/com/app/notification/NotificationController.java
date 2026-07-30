package com.app.notification;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "通知", description = "通知列表、未读数、已读标记")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取通知列表（分页，最新在前）")
    public ApiResponse<PageResult<NotificationVO>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(notificationService.getNotifications(userId, pageable));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "获取未读通知数量")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条通知为已读")
    public ApiResponse<Void> markAsRead(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long id) {
        notificationService.markAsRead(id, userId);
        return ApiResponse.success();
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记为已读")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.success();
    }
}
