package com.app.user;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器 — 用户资料管理、关注/取关
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户", description = "用户资料、关注/取关、粉丝/关注列表")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ApiResponse<UserVO> getCurrentUser(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getCurrentUser(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "更新当前用户信息")
    public ApiResponse<UserVO> updateUser(@AuthenticationPrincipal Long userId,
                                          @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.updateUser(userId, request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取指定用户信息")
    public ApiResponse<UserVO> getUserById(@PathVariable Long userId) {
        return ApiResponse.success(userService.getUserById(userId));
    }

    @PostMapping("/{userId}/follow")
    @Operation(summary = "关注用户")
    public ApiResponse<Void> follow(@AuthenticationPrincipal Long currentUserId,
                                    @PathVariable Long userId) {
        userService.follow(currentUserId, userId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{userId}/follow")
    @Operation(summary = "取关用户")
    public ApiResponse<Void> unfollow(@AuthenticationPrincipal Long currentUserId,
                                      @PathVariable Long userId) {
        userService.unfollow(currentUserId, userId);
        return ApiResponse.success();
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "获取粉丝列表（分页）")
    public ApiResponse<PageResult<UserVO>> getFollowers(@PathVariable Long userId,
                                                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(userService.getFollowers(userId, pageable));
    }

    @GetMapping("/{userId}/followees")
    @Operation(summary = "获取关注列表（分页）")
    public ApiResponse<PageResult<UserVO>> getFollowees(@PathVariable Long userId,
                                                        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(userService.getFollowees(userId, pageable));
    }
}
