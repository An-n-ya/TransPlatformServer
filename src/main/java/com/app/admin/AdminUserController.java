package com.app.admin;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import com.app.user.UserService;
import com.app.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台用户控制器 — 用户查询、粉丝/关注列表
 */
@RestController
@RequestMapping("/admin/v1/users")
@RequiredArgsConstructor
@Tag(name = "管理后台-用户", description = "管理员用户查询、粉丝/关注列表")
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前管理员信息")
    public ApiResponse<UserVO> getCurrentAdmin(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getCurrentUser(userId));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取指定用户信息")
    public ApiResponse<UserVO> getUser(@PathVariable Long userId) {
        return ApiResponse.success(userService.getUserById(userId));
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "获取指定用户的粉丝列表（分页）")
    public ApiResponse<PageResult<UserVO>> getFollowers(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(userService.getFollowers(userId, pageable));
    }

    @GetMapping("/{userId}/followees")
    @Operation(summary = "获取指定用户的关注列表（分页）")
    public ApiResponse<PageResult<UserVO>> getFollowees(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(userService.getFollowees(userId, pageable));
    }
}
