package com.app.email;

import java.time.LocalDateTime;

/**
 * 邮件发送服务
 */
public interface EmailService {

    /**
     * 发送验证码邮件
     *
     * @param to    收件人邮箱
     * @param code  6 位验证码
     * @param scene 使用场景（验证邮箱 / 找回密码），用于邮件标题与内容提示
     */
    void sendVerificationCode(String to, String code, String scene);

    /**
     * 发送一次性邀请码邮件（正文包含邀请码与 Android 宣传物料图）
     *
     * @param to        收件人邮箱
     * @param code      一次性邀请码
     * @param expiredAt 邀请码过期时间
     */
    void sendInvitationCode(String to, String code, LocalDateTime expiredAt);
}
