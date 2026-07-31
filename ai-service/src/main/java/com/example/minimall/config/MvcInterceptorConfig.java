package com.example.minimall.config;

import com.example.minimall.interceptor.ApiLoggingInterceptor;
import com.example.minimall.interceptor.PerformanceInterceptor;
import com.example.minimall.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 拦截器与 CORS 配置。
 * <p>
 * 实现 {@link WebMvcConfigurer}，集中注册 ai-service 用到的通用拦截器
 * （日志、性能、限流）并配置全局 CORS 跨域策略。
 * </p>
 * <p><b>说明</b>：原单体 PermissionInterceptor（基于角色的细粒度权限控制）依赖
 * UserService/JwtUtil 等跨域组件，在 ai-service 领域裁剪后已移除。
 * ai-service 的访问控制由两层保障：
 * <ol>
 *   <li>网关层 JWT 认证（minimall-gateway 的 JwtAuthGatewayFilter）</li>
 *   <li>SecurityConfig 白名单（/api/ai/**、/api/faq/** 等公开端点）</li>
 * </ol>
 * </p>
 */
@Configuration
public class MvcInterceptorConfig implements WebMvcConfigurer {

    private final PerformanceInterceptor performanceInterceptor;
    private final ApiLoggingInterceptor apiLoggingInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public MvcInterceptorConfig(PerformanceInterceptor performanceInterceptor,
                                ApiLoggingInterceptor apiLoggingInterceptor,
                                RateLimitInterceptor rateLimitInterceptor) {
        this.performanceInterceptor = performanceInterceptor;
        this.apiLoggingInterceptor = apiLoggingInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                    "http://localhost:5173",
                    "http://localhost:5174",
                    "http://localhost:5175",
                    "http://localhost:5176"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiLoggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health", "/api/error");

        registry.addInterceptor(performanceInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health", "/api/error");

        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}
