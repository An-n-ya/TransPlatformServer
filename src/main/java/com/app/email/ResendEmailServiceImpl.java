package com.app.email;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * 基于 Resend.com REST API 的邮件发送实现。
 * <p>
 * API Key 从环境变量 {@code RESEND_API_KEY} 读取。
 */
@Slf4j
@Service
public class ResendEmailServiceImpl implements EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    /** 发件人，格式为 "名称 <邮箱>" */
    private static final String FROM = "noreply <noreply@mail.annya.work>";

    private final RestClient restClient;
    private final String apiKey;
    /** Android 宣传物料图片路径（base64 内嵌到邮件正文） */
    private final String materialImagePath;

    public ResendEmailServiceImpl(@Value("${RESEND_API_KEY:}") String apiKey,
                                  @Value("${app.invitation.material-image-path:assets/material.png}") String materialImagePath) {
        this.apiKey = apiKey;
        this.materialImagePath = materialImagePath;
        this.restClient = RestClient.builder().baseUrl(RESEND_API_URL).build();
    }

    @Override
    public void sendVerificationCode(String to, String code, String scene) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 RESEND_API_KEY，无法发送邮件");
        }

        String title = switch (scene) {
            case VerificationCodeService.SCENE_PASSWORD_RESET -> "找回密码";
            default -> "验证邮箱";
        };

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;padding:24px;border:1px solid #eee;border-radius:8px">
                  <h2 style="color:#333">%s</h2>
                  <p style="color:#555">您好，您的验证码为：</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:6px;color:#1a73e8">%s</p>
                  <p style="color:#888;font-size:13px">验证码 10 分钟内有效，请勿泄露给他人。如非本人操作，请忽略本邮件。</p>
                </div>
                """.formatted(title, code);

        ResendEmailRequest request = new ResendEmailRequest(
                FROM,
                List.of(to),
                "TransPlatform - " + title,
                html
        );

        try {
            restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Verification email sent to {} (scene={})", to, scene);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
            throw new IllegalStateException("邮件发送失败，请稍后再试", e);
        }
    }

    /**
     * 发送一次性邀请码邮件：正文包含有效邀请码（一次性）与 Android 宣传物料图片。
     * <p>
     * 宣传物料图片以 base64 data URI 内嵌，无需依赖公开 URL。
     *
     * @param to        收件人邮箱
     * @param code      一次性邀请码
     * @param expiredAt 邀请码过期时间
     */
    @Override
    public void sendInvitationCode(String to, String code, LocalDateTime expiredAt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 RESEND_API_KEY，无法发送邮件");
        }

        String materialImageHtml = buildMaterialImageHtml();
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #eee;border-radius:8px">
                  <h2 style="color:#333">您的专属邀请码</h2>
                  <p style="color:#555">您好，这是为您生成的 TransPlatform 一次性邀请码：</p>
                  <p style="font-size:26px;font-weight:bold;letter-spacing:4px;color:#1a73e8;background:#f2f6ff;padding:14px;border-radius:6px;text-align:center">%s</p>
                  <p style="color:#888;font-size:13px">邀请码有效期至 %s，仅可使用一次，请勿泄露给他人。</p>
                  %s
                  <p style="color:#aaa;font-size:12px;text-align:center;margin-top:16px">— TransPlatform</p>
                </div>
                """.formatted(code,
                expiredAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                materialImageHtml);

        ResendEmailRequest request = new ResendEmailRequest(
                FROM,
                List.of(to),
                "YX - 您的专属邀请码",
                html
        );

        try {
            restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Invitation email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send invitation email to {}: {}", to, e.getMessage());
            throw new IllegalStateException("邮件发送失败，请稍后再试", e);
        }
    }

    /** 构建 Android 宣传物料图片 HTML（base64 内嵌）；文件缺失时返回空串并降级为不插图 */
    private String buildMaterialImageHtml() {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(materialImagePath));
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "<img src=\"data:image/png;base64," + base64 + "\" alt=\"Android 宣传物料\" "
                    + "style=\"width:100%;max-width:560px;border-radius:8px;margin-top:16px\" />";
        } catch (IOException e) {
            log.warn("无法读取宣传物料图片 {}: {}", materialImagePath, e.getMessage());
            return "";
        }
    }

    /**
     * Resend API 请求体
     */
    private record ResendEmailRequest(
            String from,
            List<String> to,
            String subject,
            @JsonProperty("html") String html
    ) {
    }
}
