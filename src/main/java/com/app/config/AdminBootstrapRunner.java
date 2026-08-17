package com.app.config;

import com.app.user.User;
import com.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 引导管理员账号 — 首次启动时若无 admin 用户则自动创建
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        boolean hasAdmin = userRepository.findAll().stream()
                .anyMatch(u -> "admin".equals(u.getRole()));
        if (hasAdmin) {
            return;
        }

        User admin = new User(adminUsername, "管理员", passwordEncoder.encode(adminPassword));
        admin.setRole("admin");
        userRepository.save(admin);

        log.warn("已创建默认管理员账号: username={}, password={}（请尽快修改）",
                adminUsername, adminPassword);
    }
}
