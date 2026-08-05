package com.example.minimall.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Feign 远程调用拦截器 — 自动传递请求上下文。
 *
 * <p>场景：用户请求 → Gateway → OrderService → (Feign) → ProductService
 * <ol>
 *   <li>Gateway 解析 JWT → 将 userId 放入 X-User-Id Header → 传给 OrderService</li>
 *   <li>OrderService 通过 Feign 调用 ProductService 时，
 *       此拦截器自动从当前请求中取出 X-User-Id → 塞入 Feign 请求 Header</li>
 *   <li>ProductService 收到 Feign 请求 → 从 Header 中取 X-User-Id → 知道是谁在操作</li>
 * </ol>
 *
 * <p>不传的话 ProductService 不知道是谁在调它，权限校验会失效。
 */
@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 从当前 HTTP 请求的上下文获取原始请求
                RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
                if (attributes instanceof ServletRequestAttributes) {
                    HttpServletRequest request = 
                            ((ServletRequestAttributes) attributes).getRequest();

                    // 传递用户 ID（Gateway 注入的 X-User-Id）
                    String userId = request.getHeader("X-User-Id");
                    if (userId != null) {
                        template.header("X-User-Id", userId);
                    }

                    // 传递用户角色
                    String role = request.getHeader("X-User-Role");
                    if (role != null) {
                        template.header("X-User-Role", role);
                    }

                    // 传递 TraceId（全链路追踪）
                    String traceId = request.getHeader("X-Trace-Id");
                    if (traceId != null) {
                        template.header("X-Trace-Id", traceId);
                    }

                    log.debug("Feign 传递上下文: userId={}, traceId={}", userId, traceId);
                }
            }
        };
    }
}
