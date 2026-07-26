package com.example.minimall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商城系统启动入口（v2.0 — 微服务化改造）。
 *
 * <p>开启异步任务 {@link EnableAsync}、定时任务 {@link EnableScheduling}、缓存 {@link EnableCaching}，
 * Nacos 服务注册发现 {@link EnableDiscoveryClient}、Feign 声明式调用 {@link EnableFeignClients}，
 * 并扫描 {@code com.example.minimall.mapper} 包下的 MyBatis 映射接口。
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableDiscoveryClient      // 注册到 Nacos
@EnableFeignClients          // 启用 Feign 远程调用
@MapperScan("com.example.minimall.mapper")
public class MinimaMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinimaMallApplication.class, args);
    }
}