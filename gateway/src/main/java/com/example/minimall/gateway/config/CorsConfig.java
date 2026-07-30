package com.example.minimall.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 网关统一跨域配置。
 *
 * <p>前端三个端（admin-web:3001, seller-web:5173, web-mall:5176）都通过网关访问，
 * 跨域只在网关层处理一次，下游微服务不需要各自配 CORS。
 *
 * <p>允许的源通过环境变量 {@code CORS_ALLOWED_ORIGINS}（逗号分隔）配置，
 * 本地默认仅放行 localhost；生产环境必须显式配置为具体前端域名。
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的前端源（本地默认 localhost，生产通过 CORS_ALLOWED_ORIGINS 改为具体域名）
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-User-Id", "X-User-Role", "X-Trace-Id"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 预检请求缓存 1 小时

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
