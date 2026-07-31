package com.example.minimall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * MiniMall API 网关 — 统一入口（:8080）。
 *
 * <p>职责：
 * <ul>
 *   <li>统一鉴权 — JWT Token 校验，解析 userId 放入 Header 传给下游</li>
 *   <li>路由转发 — 根据 URL 前缀路由到对应微服务（通过 Nacos 发现）</li>
 *   <li>限流熔断 — 基于 Sentinel 的网关层流量控制</li>
 *   <li>跨域处理 — 统一 CORS 配置，前端无需各自处理</li>
 * </ul>
 *
 * <p>技术栈：Spring Cloud Gateway（WebFlux 响应式） + Nacos + Sentinel
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
