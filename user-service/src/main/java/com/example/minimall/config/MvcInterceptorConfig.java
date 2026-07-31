package com.example.minimall.config;

import com.example.minimall.interceptor.PermissionInterceptor;
import com.example.minimall.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 拦截器注册。
 *
 * <p>安全修复：此前 user-service 定义了 {@link PermissionInterceptor} 与 {@link RateLimitInterceptor}
 * 却从未注册，导致基于角色的访问控制与接口限流在运行时完全不生效
 * （任意已登录买家即可调用卖家/管理员接口，如 /api/seller/approve、/api/seller/disable）。
 * 本类将两者注册到 {@code /api/**}，与 order-service / chat-service / backend 的做法保持一致。
 *
 * <p>说明：CORS 由 {@code SecurityConfig.corsConfigurationSource()} + {@code http.cors()} 统一处理，
 * 此处不再重复配置 addCorsMappings，避免响应头重复。
 */
@Configuration
public class MvcInterceptorConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public MvcInterceptorConfig(PermissionInterceptor permissionInterceptor,
                                RateLimitInterceptor rateLimitInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * 注册拦截器：限流 -> 权限（限流在前，尽早拦截高频请求）。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");

        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**");
    }
}
