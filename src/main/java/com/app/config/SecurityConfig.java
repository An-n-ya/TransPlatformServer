package com.app.config;

import com.app.common.JwtUtil;
import com.app.user.User;
import com.app.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security 配置 — 无状态 JWT 认证 + 管理后台角色鉴权
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /** 白名单 URL — 无需认证即可访问 */
    private static final List<String> WHITELIST = List.of(
            "/api/v1/auth/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/ws/**",
            "/uploads/**"
    );

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(WHITELIST.toArray(new String[0])).permitAll()
                // 管理后台：登录/刷新放行，其余仅管理员（ROLE_ADMIN）可访问
                .requestMatchers("/admin/v1/auth/**").permitAll()
                .requestMatchers("/admin/v1/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/topics/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/search").permitAll()
                // 逆地理编码：GPS 坐标 → 城市（无需登录即可调用）
                .requestMatchers(HttpMethod.GET, "/api/v1/location/**").permitAll()
                // 用户自己的相关接口（/me/**）必须登录，需在宽泛的公开规则之前匹配
                .requestMatchers(HttpMethod.GET, "/api/v1/users/me/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
                })
                // 已登录但权限不足时直接返回 403（避免默认 sendError 触发 /error 二次分发）
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write("{\"code\":403,\"message\":\"无权访问此资源\",\"data\":null}");
                }))
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** JWT 认证过滤器 — 从请求头提取并验证 Token，管理后台路径加载真实角色 */
    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String header = request.getHeader("Authorization");
                if (header != null && header.startsWith("Bearer ")) {
                    String token = header.substring(7);
                    try {
                        if (jwtUtil.validateToken(token) && !jwtUtil.isRefreshToken(token)) {
                            Long userId = jwtUtil.getUserIdFromToken(token);
                            // 仅管理后台路径需要从数据库加载真实角色（其余默认普通用户）
                            String role = "user";
                            if (request.getRequestURI().startsWith("/admin/")) {
                                role = userRepository.findById(userId)
                                        .map(User::getRole)
                                        .orElse("user");
                            }
                            JwtAuthentication authentication = new JwtAuthentication(userId, role);
                            org.springframework.security.core.context.SecurityContextHolder
                                    .getContext().setAuthentication(authentication);
                        }
                    } catch (Exception e) {
                        log.warn("JWT authentication failed: {}", e.getMessage());
                    }
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
