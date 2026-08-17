package com.app.invitation;

import java.util.List;

/**
 * 邀请码 Service 接口
 */
public interface InvitationService {

    /**
     * 生成邀请码（仅 admin 用户可调用）
     *
     * @param inviterId 当前用户 ID
     * @param count     生成数量
     * @param days      有效期（天）
     * @param scene     场景标识
     * @return 生成的邀请码列表
     */
    List<InvitationVO> generate(Long inviterId, int count, int days, String scene);

    /**
     * 校验并消耗邀请码（注册时调用）
     *
     * @param code      邀请码
     * @param inviteeId 被邀请人（注册后的用户 ID）
     */
    void validateAndUse(String code, Long inviteeId);
}
