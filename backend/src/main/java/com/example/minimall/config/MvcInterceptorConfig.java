package com.example.minimall.config;

import com.example.minimall.interceptor.ApiLoggingInterceptor;
import com.example.minimall.interceptor.PerformanceInterceptor;
import com.example.minimall.interceptor.PermissionInterceptor;
import com.example.minimall.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 拦截器与 CORS 配置。
 * <p>
 * 实现 {@link WebMvcConfigurer}，集中注册项目用到的全部拦截器
 * （日志、性能、限流、权限）并配置全局 CORS 跨域策略。
 * </p>
 */
@Configuration
public class MvcInterceptorConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;
    private final PerformanceInterceptor performanceInterceptor;
    private final ApiLoggingInterceptor apiLoggingInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 构造方法，注入全部自定义拦截器实例。
     */
    public MvcInterceptorConfig(PermissionInterceptor permissionInterceptor,
                                PerformanceInterceptor performanceInterceptor,
                                ApiLoggingInterceptor apiLoggingInterceptor,
                                RateLimitInterceptor rateLimitInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
        this.performanceInterceptor = performanceInterceptor;
        this.apiLoggingInterceptor = apiLoggingInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * 配置全局 CORS 跨域规则：放行本机常见前端端口与常用 HTTP 方法。
     */
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

    /**
     * 注册并按顺序应用各拦截器：日志 -> 性能 -> 限流 -> 权限。
     */
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

        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**");
    }
}
