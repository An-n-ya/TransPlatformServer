package com.app.invitation;

import com.app.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 邀请码控制器
 */
@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "邀请码", description = "生成邀请码（仅管理员）")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    @Operation(summary = "生成邀请码（仅管理员）")
    public ApiResponse<List<InvitationVO>> generate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GenerateInvitationRequest request) {
        return ApiResponse.success(invitationService.generate(
                userId, request.getCount(), request.getDays(), request.getScene()));
    }
}
