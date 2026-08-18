package com.app.email;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 验证码服务 — 生成、发送与校验验证码。
 * <p>
 * 验证码通过 Redis 存储，10 分钟内有效；同一邮箱/场景有 60 秒发送冷却与错误次数限制。
 */
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    /** 场景：验证邮箱 */
    public static final String SCENE_EMAIL = "email";
    /** 场景：找回密码 */
    public static final String SCENE_PASSWORD_RESET = "password_reset";

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    /**
     * 生成并发送验证码
     *
     * @param email 目标邮箱
     * @param scene 使用场景
     */
    public void sendCode(String email, String scene) {
        String cooldownKey = "verify:cooldown:" + scene + ":" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new IllegalStateException("发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        redisTemplate.opsForValue().set(codeKey(scene, email), code, CODE_TTL);
        redisTemplate.opsForValue().set(cooldownKey, "1", SEND_COOLDOWN);
        redisTemplate.delete(attemptKey(scene, email));

        emailService.sendVerificationCode(email, code, scene);
    }

    /**
     * 校验验证码，校验成功后消费（删除），不可复用
     *
     * @param email 目标邮箱
     * @param scene 使用场景
     * @param code  用户输入的验证码
     */
    public void verifyCode(String email, String scene, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("验证码不能为空");
        }

        String key = codeKey(scene, email);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            throw new IllegalStateException("验证码不存在或已过期，请重新获取");
        }

        // 校验错误次数限制，防止暴力破解
        String attemptKey = attemptKey(scene, email);
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptKey, CODE_TTL);
        }
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redisTemplate.delete(key);
            throw new IllegalStateException("验证码错误次数过多，请重新获取");
        }

        if (!stored.equals(code)) {
            throw new IllegalArgumentException("验证码错误");
        }

        // 校验成功，消费验证码
        redisTemplate.delete(key);
        redisTemplate.delete(attemptKey);
    }

    private String codeKey(String scene, String email) {
        return "verify:code:" + scene + ":" + email.toLowerCase();
    }

    private String attemptKey(String scene, String email) {
        return "verify:attempt:" + scene + ":" + email.toLowerCase();
    }
}
