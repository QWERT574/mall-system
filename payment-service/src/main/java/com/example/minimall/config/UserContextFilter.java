package com.example.minimall.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * 用户上下文过滤器 — 从网关注入的 Header 中提取用户信息。
 *
 * <p>架构约定：
 * <ul>
 *   <li>网关 (Gateway) 负责 JWT 解析 → 将 userId/role 注入 X-User-Id / X-User-Role Header</li>
 *   <li>下游微服务不再解析 JWT，直接信任网关传入的 Header</li>
 *   <li>如果 Header 缺失（如内部调用），设置为匿名用户</li>
 * </ul>
 *
 * <p>替代原来各服务冗余的 JwtAuthenticationFilter，避免网关剥 Authorization 后下游二次验证失效。
 *
 * <p><b>安全桥接</b>：网关注入 X-User-Id 后，下游 Spring Security 的 {@code .authenticated()} 仍要求
 * SecurityContext 中存在 Authentication 对象。本过滤器在设置 ThreadLocal 的同时，将网关预认证的用户身份
 * 转为 {@link UsernamePasswordAuthenticationToken} 写入 {@link SecurityContextHolder}，
 * 使 SecurityConfig 的授权规则与网关注入的身份对齐，避免非白名单接口被 403 拦截。
 */
@Slf4j
@Component
public class UserContextFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_ROLE = new ThreadLocal<>();

    /** 网关注入的内部令牌；仅当请求携带匹配的内部令牌时才信任 X-User-Id 身份头，防止直连伪造 */
    @org.springframework.beans.factory.annotation.Value("${internal.token:minimall-internal-token-dev}")
    private String internalToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            String userIdStr = request.getHeader(HEADER_USER_ID);
            String role = request.getHeader(HEADER_USER_ROLE);
            String tokenHeader = request.getHeader(HEADER_INTERNAL_TOKEN);

            // 仅当内部令牌匹配时才信任网关注入的 X-User-Id，防止绕过网关直连服务端口伪造身份
            if (userIdStr != null && !userIdStr.isEmpty()
                    && internalToken != null && internalToken.equals(tokenHeader)) {
                Long userId = Long.parseLong(userIdStr);
                String userRole = role != null ? role : "USER";
                CURRENT_USER_ID.set(userId);
                CURRENT_USER_ROLE.set(userRole);
                // 桥接 Spring Security：网关已完成 JWT 认证，此处将预认证身份注入 SecurityContext，
                // 使 .anyRequest().authenticated() 放行（principal=userId, credential=N/A, authority=ROLE_userRole）
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("UserContext: userId={}, role={}", userIdStr, role);
            }

            filterChain.doFilter(request, response);

        } finally {
            CURRENT_USER_ID.remove();
            CURRENT_USER_ROLE.remove();
            SecurityContextHolder.clearContext();
        }
    }

    /** 获取当前请求的用户 ID（由网关注入） */
    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /** 获取当前请求的用户角色 */
    public static String getCurrentUserRole() {
        return CURRENT_USER_ROLE.get();
    }
}
