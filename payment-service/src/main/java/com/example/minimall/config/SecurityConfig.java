package com.example.minimall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 安全配置。
 * <p>
 * 基于 JWT 的无状态安全方案：禁用 CSRF/HttpSession，按接口白名单放行公开 API，
 * 业务级角色控制交给 {@code PermissionInterceptor}。
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserContextFilter userContextFilter;

    public SecurityConfig(UserContextFilter userContextFilter) {
        this.userContextFilter = userContextFilter;
    }

    /**
     * 装配 BCrypt 密码编码器，用于用户密码加密与校验。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 装配安全过滤链：禁用 CSRF 与 Session、配置白名单与认证策略、注册 UserContext 过滤器与 CORS。
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF
            .csrf().disable()
            // 禁用 Session
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 配置授权规则
            .authorizeRequests()
                // ===== 内部 API（Feign 服务间调用，不经网关）=====
                .antMatchers("/api/internal/**").permitAll()
                // ===== 认证相关（公开） =====
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/captcha/**").permitAll()
                .antMatchers("/api/sms/**").permitAll()
                // ===== 商品/分类/活动/优惠 公开浏览 =====
                .antMatchers("/api/product/list").permitAll()
                .antMatchers("/api/product/search").permitAll()
                .antMatchers("/api/product/category/**").permitAll()
                .antMatchers("/api/product/recommended").permitAll()
                .antMatchers("/api/product/hot").permitAll()
                .antMatchers("/api/product/*").permitAll()         // /api/product/{id} 详情
                .antMatchers("/api/product/*/specs").permitAll()   // /api/product/{id}/specs 规格
                .antMatchers("/api/category/**").permitAll()
                .antMatchers("/api/activity/list").permitAll()
                .antMatchers("/api/activity/recommended").permitAll()
                .antMatchers("/api/activity/*").permitAll()         // /api/activity/{id} 详情
                .antMatchers("/api/coupon/available").permitAll()
                .antMatchers("/api/coupon/list").permitAll()
                .antMatchers("/api/coupon/calculate/*").permitAll()
                .antMatchers("/api/discount/list").permitAll()
                .antMatchers("/api/discount/active").permitAll()
                .antMatchers("/api/discount/active-with-products").permitAll()
                .antMatchers("/api/discount/*").permitAll()         // /api/discount/{id} 详情
                // ===== 评价公开浏览 =====
                .antMatchers("/api/review/list").permitAll()
                .antMatchers("/api/review/product/**").permitAll()
                // ===== AI 客服 + FAQ + 会话入口 =====
                .antMatchers("/api/ai/**").permitAll()
                .antMatchers("/api/faq/**").permitAll()
                .antMatchers("/api/cs/**").permitAll()
                // ===== 文件上传/下载 + 静态资源 =====
                .antMatchers("/api/upload/**").permitAll()
                .antMatchers("/uploads/**").permitAll()
                .antMatchers("/images/**").permitAll()
                // ===== WebSocket 端点 =====
                .antMatchers("/ws-chat/**").permitAll()
                // ===== 可观测性端点 =====
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/actuator/**").hasIpAddress("127.0.0.1")
                // ===== 其他所有接口均需认证 =====
                .anyRequest().authenticated()
            .and()
            // 添加 UserContext 过滤器（双模式：网关 X-User-Id 头优先，无则 fallback 解 JWT）
            .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class)
            // 配置 CORS
            .cors().configurationSource(corsConfigurationSource());

        return http.build();
    }

    /**
     * 允许的前端源：本地默认仅 localhost，生产通过环境变量 CORS_ALLOWED_ORIGINS（逗号分隔）收敛为具体域名。
     */
    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
    private String corsAllowedOrigins;

    /**
     * 装配 CORS 配置源：放行受信任来源，支持凭据与常用 HTTP 方法。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-User-Id", "X-User-Role", "X-Trace-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
