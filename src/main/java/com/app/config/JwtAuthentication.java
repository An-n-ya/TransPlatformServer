package com.app.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * 自定义 JWT 认证令牌 — 持有 userId 作为 principal，role 决定授权（ROLE_ADMIN / ROLE_USER）
 */
public class JwtAuthentication extends AbstractAuthenticationToken {

    private final Long userId;
    private final String role;

    public JwtAuthentication(Long userId, String role) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
        this.userId = userId;
        this.role = role;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Long getPrincipal() {
        return userId;
    }
}
