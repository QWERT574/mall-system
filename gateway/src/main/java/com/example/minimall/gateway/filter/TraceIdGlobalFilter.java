package com.example.minimall.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关链路追踪过滤器 — 微服务路径 traceId 的统一入口。
 *
 * <p>与单体/order-service 的 TraceIdFilter 使用同一头名与格式：
 * <ol>
 *   <li>优先透传客户端已携带的 X-Trace-Id 头</li>
 *   <li>否则生成去连字符的 UUID 作为本次请求的 traceId</li>
 *   <li>写入下游请求头 — 下游服务的 TraceIdFilter 读取后放入 MDC</li>
 *   <li>回写到响应头 — 便于客户端/排障时按 traceId 检索跨服务日志</li>
 * </ol>
 *
 * <p>为什么放网关？→ 一次请求跨 gateway → service → Feign 需要同一相关键，
 * 网关是链路起点，在这里注入才能保证全链路使用同一个 traceId。
 */
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    /** 与 order-service TraceIdFilter.HEADER_TRACE_ID 保持一致 */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        final String finalTraceId = traceId;
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER_TRACE_ID, finalTraceId))
                .build();
        exchange.getResponse().getHeaders().set(HEADER_TRACE_ID, finalTraceId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // 在 JwtAuthGatewayFilter(-1) 之前执行，保证鉴权阶段的日志/响应也携带 traceId
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
