package com.example.minimall.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.util.AntPathMatcher;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 网关全局 JWT 鉴权过滤器。
 *
 * <p>设计思路：
 * <ol>
 *   <li>白名单路径直接放行（登录、注册、公开商品查询等）</li>
 *   <li>非白名单路径必须携带有效 JWT Token（Authorization: Bearer xxx）</li>
 *   <li>校验通过后，从 Token 中提取 userId 和 role，放入 Header 传给下游服务</li>
 *   <li>下游服务不再需要解析 JWT，直接从 Header 取 userId（减少重复解析）</li>
 * </ol>
 *
 * <p>为什么放网关而不是每个微服务各自做？
 * → 统一的入口做鉴权，下游服务信任网关传过来的 Header 即可，
 *    避免每个服务都依赖 JWT 库，也避免密钥分散管理。
 */
@Slf4j
@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 白名单 — 不需要 Token 就能访问的路径。
     * 与后端 SecurityConfig 的白名单保持一致。
     */
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            // 认证相关
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/sms-login",
            "/api/auth/wx-login",
            "/api/captcha/**",
            // 公开数据（Ant 格式：** 匹配子路径）
            "/api/product/list/**",
            "/api/product/detail/**",
            "/api/category/**",
            "/api/activity/**",
            "/api/coupon/public/**",
            "/api/coupon/list",
            "/api/coupon/available",
            "/api/discount/list",
            "/api/discount/active",
            // 监控端点
            "/actuator/**",
            // 文件访问
            "/uploads/**",
            "/images/**",
            // AI/FAQ 公开（仅客服问答端点；logs/monitor 等管理端点仍需认证）
            "/api/ai/query",
            "/api/ai/chat",
            "/api/ai/rag-query",
            "/api/ai/rag-chat",
            "/api/faq/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // ── 0. 安全加固：无条件剥离客户端自带的身份头，防止伪造 X-User-Id/X-User-Role 冒充任意用户 ──
        ServerHttpRequest cleanedRequest = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Role");
                })
                .build();
        exchange = exchange.mutate().request(cleanedRequest).build();

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // ── 1. 白名单放行 ──
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // ── 2. 提取 Token ──
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少认证令牌");
        }

        String token = authHeader.substring(7); // 去掉 "Bearer " 前缀

        // ── 3. 校验 Token ──
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);

            if (userId == null) {
                return unauthorized(exchange, "令牌无效：缺少用户标识");
            }

            // ── 4. 将用户信息注入 Header，传给下游服务 ──
            // 注：保留 Authorization 头，下游 PermissionInterceptor 需要从 JWT 中解析 userType（0/1/2）
            // 网关已校验 JWT 合法性，下游不再重复校验签名，仅提取 userType 做角色白名单控制
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-User-Role", role != null ? role : "USER")
                    .build();

            log.debug("JWT 校验通过 → userId={}, role={}, path={}", userId, role, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            return unauthorized(exchange, "令牌已过期，请重新登录");
        } catch (SignatureException | MalformedJwtException e) {
            return unauthorized(exchange, "令牌无效");
        } catch (Exception e) {
            log.error("JWT 校验异常: {}", e.getMessage());
            return unauthorized(exchange, "认证服务异常");
        }
    }

    @Override
    public int getOrder() {
        // -1：在 Netty 路由之前执行（最早触发）
        return -1;
    }

    // ── 辅助方法 ──

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 判断路径是否在白名单中（使用 Ant 风格精确匹配，防绕过）。
     */
    private boolean isWhitelisted(String path) {
        return WHITELIST_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 返回 401 Unauthorized JSON 响应。
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"code\":401,\"message\":\"%s\",\"data\":null}", message);
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
