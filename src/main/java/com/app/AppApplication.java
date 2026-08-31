package com.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
@EnableAsync
public class AppApplication {

    public static void main(String[] args) {
        /*
         * 固定 JVM 默认时区，在 Spring 容器启动前生效。
         *
         * 背景：BaseEntity 及各类实体用 LocalDateTime.now() 生成时间戳，Hibernate
         * 在 LocalDateTime <-> epoch 毫秒之间转换时也依赖 JVM 默认时区。本地开发机默认
         * 是 Asia/Shanghai 所以正确；但 Docker 容器（eclipse-temurin）默认 UTC，导致
         * 部署到服务器后接口返回的时间比东八区慢/快 8 小时。
         *
         * 这里统一把 JVM 默认时区固定为东八区，保证任何部署环境行为一致。
         * 如需覆盖，可通过环境变量 APP_TIMEZONE 或系统属性 app.timezone 指定（如 UTC）。
         */
        String tz = System.getProperty("app.timezone",
                System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Shanghai"));
        TimeZone.setDefault(TimeZone.getTimeZone(tz));
        System.setProperty("user.timezone", tz);

        SpringApplication.run(AppApplication.class, args);
    }
}
