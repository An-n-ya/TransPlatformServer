package com.app.invitation;

import com.app.user.User;
import com.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private static final String ROLE_ADMIN = "admin";
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去除易混淆字符 0/O/1/I
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<InvitationVO> generate(Long inviterId, int count, int days, String scene) {
        // 权限校验：仅 admin 可生成邀请码
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!ROLE_ADMIN.equals(inviter.getRole())) {
            throw new SecurityException("仅管理员可以生成邀请码");
        }

        LocalDateTime expiredAt = LocalDateTime.now().plusDays(days);
        List<InvitationVO> result = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Invitation inv = new Invitation(generateCode(), inviterId, expiredAt, scene);
            invitationRepository.save(inv);
            result.add(toVO(inv));
        }

        log.info("Invitations generated: count={}, inviterId={}, days={}, scene={}", count, inviterId, days, scene);
        return result;
    }

    @Override
    @Transactional
    public void validateAndUse(String code, Long inviteeId) {
        Invitation inv = invitationRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("邀请码无效"));

        if (inv.getStatus() == Invitation.STATUS_USED) {
            throw new IllegalArgumentException("邀请码已被使用");
        }
        if (inv.getStatus() == Invitation.STATUS_EXPIRED) {
            throw new IllegalArgumentException("邀请码已过期");
        }
        if (inv.getExpiredAt().isBefore(LocalDateTime.now())) {
            inv.setStatus(Invitation.STATUS_EXPIRED);
            invitationRepository.save(inv);
            throw new IllegalArgumentException("邀请码已过期");
        }

        // 消耗邀请码
        inv.setStatus(Invitation.STATUS_USED);
        inv.setInviteeId(inviteeId);
        invitationRepository.save(inv);
        log.info("Invitation used: code={}, inviteeId={}", code, inviteeId);
    }

    /** 生成 16 位邀请码 */
    private String generateCode() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private InvitationVO toVO(Invitation inv) {
        return InvitationVO.builder()
                .id(inv.getId())
                .code(inv.getCode())
                .inviterId(inv.getInviterId())
                .inviteeId(inv.getInviteeId())
                .status(inv.getStatus())
                .expiredAt(inv.getExpiredAt())
                .scene(inv.getScene())
                .createdAt(inv.getCreatedAt())
                .build();
    }
}
