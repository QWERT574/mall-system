package com.example.minimall.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关路由冒烟测试 — 不依赖 Nacos/Sentinel/下游服务。
 *
 * <p>覆盖三条最小断言链：
 * <ol>
 *   <li>上下文可加载：application.yml 全部路由定义合法</li>
 *   <li>traceId 注入：任意请求的响应头都携带 X-Trace-Id（无则生成、有则透传）</li>
 *   <li>JWT 鉴权边界：白名单路径放行（无实例时 503），非白名单无 Token 返回 401</li>
 * </ol>
 *
 * <p>说明：服务发现被禁用，lb:// 路由无可用实例是预期行为（503 SERVICE_UNAVAILABLE），
 * 这正好证明"请求命中了对应路由并进入负载均衡"，而不是被网关直接拒绝。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // 单测不连 Nacos / Sentinel
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.sentinel.enabled=false",
                // application.yml 中 jwt.secret 无默认值，测试提供占位密钥
                "jwt.secret=unit-test-secret-0123456789-0123456789"
        })
class GatewayRouteSmokeTest {

    @Autowired
    private WebTestClient webTestClient;

    /** 白名单路径：应命中 product-service 路由（无实例 → 503），且响应头带生成的 traceId */
    @Test
    void whitelistedRouteIsMatchedAndTraceIdGenerated() {
        webTestClient.get().uri("/api/product/list/1")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().exists("X-Trace-Id");
    }

    /** 客户端已带 X-Trace-Id：网关必须原样透传，不得重新生成 */
    @Test
    void existingTraceIdIsPropagatedUnchanged() {
        webTestClient.get().uri("/api/product/list/1")
                .header("X-Trace-Id", "smoke-test-trace-id-001")
                .exchange()
                .expectHeader().valueEquals("X-Trace-Id", "smoke-test-trace-id-001");
    }

    /** 非白名单路径无 Token：JwtAuthGatewayFilter 返回 401，且 401 响应同样携带 traceId */
    @Test
    void protectedRouteWithoutTokenIsRejectedWithTraceId() {
        webTestClient.get().uri("/api/order/list")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().exists("X-Trace-Id")
                .expectBody()
                .jsonPath("$.code").isEqualTo(401);
    }

    /** 生成的 traceId 应为去连字符 UUID 格式（32 位十六进制），与单体侧格式一致 */
    @Test
    void generatedTraceIdUsesDashlessUuidFormat() {
        String traceId = webTestClient.get().uri("/api/order/list")
                .exchange()
                .returnResult(String.class)
                .getResponseHeaders()
                .getFirst("X-Trace-Id");
        assertThat(traceId).isNotNull().matches("[0-9a-f]{32}");
    }
}
