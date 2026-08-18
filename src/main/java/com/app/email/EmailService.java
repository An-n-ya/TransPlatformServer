package com.app.email;

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
}
