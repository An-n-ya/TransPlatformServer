package com.app.email;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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

    public ResendEmailServiceImpl(@Value("${RESEND_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
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
