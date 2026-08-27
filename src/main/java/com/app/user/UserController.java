package com.app.user;

import com.app.common.ApiResponse;
import com.app.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户控制器 — 用户资料管理、关注/取关
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户", description = "用户资料、关注/取关、粉丝/关注列表")
public class UserController {

    private final UserService userService;

    @GetMapping("/check-username")
    @Operation(summary = "校验用户名是否可用")
    public ApiResponse<Map<String, Object>> checkUsername(
            @RequestParam @NotBlank(message = "用户名不能为空") String username) {
        boolean taken = userService.isUsernameTaken(username);
        return ApiResponse.success(Map.of(
                "username", username,
                "available", !taken));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ApiResponse<UserVO> getCurrentUser(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getCurrentUser(userId));
    }

    @PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "更新当前用户信息（JSON 方式）")
    public ApiResponse<UserVO> updateUser(@AuthenticationPrincipal Long userId,
                                          @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.updateUser(userId, request));
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "更新当前用户信息（multipart 方式，支持上传头像和主页背景图）")
    public ApiResponse<UserVO> updateUserMultipart(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "bioHeaderImg", required = false) MultipartFile bioHeaderImgFile,
            @RequestParam(value = "avatar", required = false) MultipartFile avatarFile) {
        return ApiResponse.success(userService.updateUser(userId, nickname, bio, location, bioHeaderImgFile, avatarFile));
    }

    @PutMapping("/me/pinned-post")
    @Operation(summary = "设置置顶帖")
    public ApiResponse<UserVO> setPinnedPost(@AuthenticationPrincipal Long userId,
                                             @RequestBody @Valid SetPinnedPostRequest request) {
        return ApiResponse.success(userService.setPinnedPost(userId, request.getPostId()));
    }

    @PostMapping("/me/email/send-code")
    @Operation(summary = "发送邮箱验证码（验证/绑定邮箱）")
    public ApiResponse<Void> sendEmailCode(@AuthenticationPrincipal Long userId,
                                           @Valid @RequestBody EmailRequest request) {
        userService.sendEmailVerificationCode(userId, request.getEmail());
        return ApiResponse.success();
    }

    @PostMapping("/me/email/verify")
    @Operation(summary = "校验验证码并绑定邮箱")
    public ApiResponse<UserVO> verifyEmail(@AuthenticationPrincipal Long userId,
                                           @Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.success(userService.verifyEmail(userId, request.getEmail(), request.getCode()));
    }

    @DeleteMapping("/me/pinned-post")
    @Operation(summary = "取消置顶帖")
    public ApiResponse<UserVO> clearPinnedPost(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.clearPinnedPost(userId));
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
