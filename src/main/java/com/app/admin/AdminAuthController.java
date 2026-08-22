package com.app.admin;

import com.app.common.ApiResponse;
import com.app.user.AuthResponse;
import com.app.user.LoginRequest;
import com.app.user.RefreshTokenRequest;
import com.app.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台认证控制器 — 管理员登录 / 刷新 Token
 */
@RestController
@RequestMapping("/admin/v1/auth")
@RequiredArgsConstructor
@Tag(name = "管理后台-认证", description = "管理员登录、刷新 Token（仅 admin 角色）")
public class AdminAuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.adminLogin(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新管理员 Token")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(userService.adminRefreshToken(request.getRefreshToken()));
    }
}
