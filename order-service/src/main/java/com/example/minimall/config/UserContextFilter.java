package com.example.minimall.config;

import com.example.minimall.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户上下文过滤器 — 从网关 Header 或 JWT Token 中提取用户信息。
 *
 * <p>双模式：
 * <ul>
 *   <li>网关模式：优先读 X-User-Id / X-User-Role（网关已解析 JWT）</li>
 *   <li>直连模式：Header 中无 X-User-Id 时，从 Authorization Bearer Token 解析 JWT</li>
 * </ul>
 */
@Slf4j
@Component
public class UserContextFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_ROLE = new ThreadLocal<>();

    private final JwtUtil jwtUtil;

    /** 网关注入的内部令牌；仅当请求携带匹配的内部令牌时才信任 X-User-Id 身份头，防止直连伪造 */
    @org.springframework.beans.factory.annotation.Value("${internal.token:minimall-internal-token-dev}")
    private String internalToken;

    public UserContextFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            String userIdStr = request.getHeader(HEADER_USER_ID);
            String role = request.getHeader(HEADER_USER_ROLE);
            String tokenHeader = request.getHeader(HEADER_INTERNAL_TOKEN);
            // 仅当内部令牌匹配时才信任网关注入的 X-User-Id，防止绕过网关直连服务端口伪造身份
            boolean trustedGateway = userIdStr != null && !userIdStr.isEmpty()
                    && internalToken != null && internalToken.equals(tokenHeader);

            if (trustedGateway) {
                // 网关模式（已校验内部令牌）
                CURRENT_USER_ID.set(Long.parseLong(userIdStr));
                CURRENT_USER_ROLE.set(role != null ? role : "USER");
                // 设置 Spring Security Context（否则 SecurityConfig.authenticated() 会拦截）
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userIdStr, null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")))));
            } else {
                // 直连模式：从 Authorization header 解析 JWT
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    try {
                        String token = authHeader.substring(7);
                        Long userId = jwtUtil.getUserIdFromToken(token);
                        if (userId != null) {
                            CURRENT_USER_ID.set(userId);
                            CURRENT_USER_ROLE.set("USER");
                            SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userId, null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))));
                        }
                    } catch (Exception e) {
                        log.debug("JWT parse failed in direct mode: {}", e.getMessage());
                    }
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            CURRENT_USER_ID.remove();
            CURRENT_USER_ROLE.remove();
        }
    }

    /** 获取当前请求的用户 ID */
    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /** 获取当前请求的用户角色 */
    public static String getCurrentUserRole() {
        return CURRENT_USER_ROLE.get();
    }
}
