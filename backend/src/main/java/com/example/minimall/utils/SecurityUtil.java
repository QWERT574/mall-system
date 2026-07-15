package com.example.minimall.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security 上下文工具类
 * <p>
 * 统一从 {@link SecurityContextHolder} 中读取当前登录用户信息。
 * 配合 {@link com.example.minimall.config.JwtAuthenticationFilter} 使用：
 * 过滤器解析 JWT 后会把 userId 写入 Authentication 的 principal。
 * </p>
 */
@Component
public class SecurityUtil {

    /**
     * 获取当前登录用户的 userId
     * <p>
     * 返回 null 的常见场景：
     * <ul>
     *   <li>请求未经过 JWT 过滤器（如白名单 URL）</li>
     *   <li>用户匿名访问（principal 为 "anonymousUser"）</li>
     *   <li>未提供有效 Token</li>
     * </ul>
     * </p>
     *
     * @return 当前登录用户 ID；未登录时返回 null
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal == null) {
            return null;
        }

        // 兼容 "anonymousUser" 字符串（Spring Security 默认匿名用户）
        if (principal instanceof String && "anonymousUser".equals(principal)) {
            return null;
        }

        // JwtAuthenticationFilter 把 userId 作为 principal 写入（Long 类型）
        if (principal instanceof Long) {
            return (Long) principal;
        }
        if (principal instanceof Number) {
            return ((Number) principal).longValue();
        }

        // 兜底：兼容纯数字字符串
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取当前登录用户名（username claim）
     * <p>注意：当前 {@link com.example.minimall.config.JwtAuthenticationFilter}
     * 写入的是 userId，所以此方法在没有 username 上下文时返回 null。</p>
     *
     * @return 用户名；未登录或上下文无 username 时返回 null
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal == null) {
            return null;
        }
        return principal.toString();
    }

    /**
     * 判断当前是否已登录
     *
     * @return 已登录返回 true
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }
}
