package com.app.admin;

import com.app.common.ApiResponse;
import com.app.email.EmailService;
import com.app.invitation.GenerateInvitationRequest;
import com.app.invitation.InvitationService;
import com.app.invitation.InvitationVO;
import com.app.invitation.SendInvitationEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台邀请码控制器 — 生成邀请码 / 生成并邮件发送一次性邀请码（仅管理员）
 */
@RestController
@RequestMapping("/admin/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "管理后台-邀请码", description = "管理员生成邀请码")
public class AdminInvitationController {

    private final InvitationService invitationService;
    private final EmailService emailService;

    @PostMapping
    @Operation(summary = "管理员生成邀请码")
    public ApiResponse<List<InvitationVO>> generate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GenerateInvitationRequest request) {
        return ApiResponse.success(invitationService.generate(
                userId, request.getCount(), request.getDays(), request.getScene()));
    }

    @PostMapping("/send-email")
    @Operation(summary = "生成一次性邀请码并发送邮件（仅管理员）")
    public ApiResponse<InvitationVO> sendInvitationEmail(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SendInvitationEmailRequest request) {
        // 生成一个一次性邀请码，并将其邮件发送到目标邮箱
        InvitationVO invitation = invitationService.generate(
                userId, 1, request.getDays(), request.getScene()).get(0);
        emailService.sendInvitationCode(request.getEmail(), invitation.getCode(), invitation.getExpiredAt());
        return ApiResponse.success(invitation);
    }
}
