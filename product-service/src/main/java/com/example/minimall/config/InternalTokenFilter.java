package com.example.minimall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 内部 API 保护过滤器 — /api/internal/** 仅允许携带正确 X-Internal-Token 的服务间调用。
 *
 * <p>令牌由环境变量 INTERNAL_TOKEN 注入（Spring 宽松绑定 → internal.token），
 * 所有微服务共享同一值；调用方通过 FeignInternalTokenConfig 自动附带该头。
 * 生产环境务必在 .env 中设置强随机 INTERNAL_TOKEN 覆盖开发默认值。
 */
@Component
@Order(1)
public class InternalTokenFilter extends OncePerRequestFilter {

    @Value("${internal.token:minimall-internal-token-dev}")
    private String internalToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/internal/")) {
            String token = request.getHeader("X-Internal-Token");
            if (token == null || !token.equals(internalToken)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"内部接口禁止外部访问\",\"data\":null}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
