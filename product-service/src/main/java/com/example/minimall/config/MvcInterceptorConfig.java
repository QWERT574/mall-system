package com.example.minimall.config;

import com.example.minimall.interceptor.RoleWriteInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 拦截器注册。
 *
 * <p>安全修复：product-service 此前没有任何角色校验（仅 SecurityConfig 的 .authenticated()），
 * 导致已登录买家可越权创建/修改商品、优惠券、折扣、活动等。本类注册
 * {@link RoleWriteInterceptor} 到 {@code /api/**}，对写操作强制卖家/管理员角色。
 *
 * <p>说明：CORS 由网关统一处理，此处不配置 addCorsMappings。
 */
@Configuration
public class MvcInterceptorConfig implements WebMvcConfigurer {

    private final RoleWriteInterceptor roleWriteInterceptor;

    public MvcInterceptorConfig(RoleWriteInterceptor roleWriteInterceptor) {
        this.roleWriteInterceptor = roleWriteInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleWriteInterceptor)
                .addPathPatterns("/api/**");
    }
}
