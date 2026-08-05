package com.example.minimall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.access.IpAddressAuthorizationManager;
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
            // 禁用 CSRF（Boot 3 / Security 6 改为 lambda DSL：csrf(csrf -> csrf.disable())）
            .csrf(csrf -> csrf.disable())
            // 禁用 Session
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置授权规则（Security 6: authorizeRequests -> authorizeHttpRequests, antMatchers -> requestMatchers）
            .authorizeHttpRequests(auth -> auth
                // ===== 认证相关（公开） =====
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/captcha/**").permitAll()
                .requestMatchers("/api/sms/**").permitAll()
                // ===== 商品/分类/活动/优惠 公开浏览 =====
                .requestMatchers("/api/product/list").permitAll()
                .requestMatchers("/api/product/search").permitAll()
                .requestMatchers("/api/product/category/**").permitAll()
                .requestMatchers("/api/product/recommended").permitAll()
                .requestMatchers("/api/product/hot").permitAll()
                .requestMatchers("/api/product/*").permitAll()         // /api/product/{id} 详情
                .requestMatchers("/api/product/*/specs").permitAll()   // /api/product/{id}/specs 规格
                .requestMatchers("/api/category/**").permitAll()
                .requestMatchers("/api/activity/list").permitAll()
                .requestMatchers("/api/activity/recommended").permitAll()
                .requestMatchers("/api/activity/*").permitAll()         // /api/activity/{id} 详情
                .requestMatchers("/api/coupon/available").permitAll()
                .requestMatchers("/api/coupon/list").permitAll()
                .requestMatchers("/api/coupon/calculate/*").permitAll()
                .requestMatchers("/api/discount/list").permitAll()
                .requestMatchers("/api/discount/active").permitAll()
                .requestMatchers("/api/discount/active-with-products").permitAll()
                .requestMatchers("/api/discount/*").permitAll()         // /api/discount/{id} 详情
                // ===== 评价公开浏览 =====
                .requestMatchers("/api/review/list").permitAll()
                .requestMatchers("/api/review/product/**").permitAll()
                // ===== AI 客服 + FAQ + 会话入口 =====
                .requestMatchers("/api/ai/**").permitAll()
                .requestMatchers("/api/faq/**").permitAll()
                .requestMatchers("/api/cs/**").permitAll()
                // ===== 文件上传/下载 + 静态资源 =====
                .requestMatchers("/api/upload/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                // ===== WebSocket 端点 =====
                .requestMatchers("/ws-chat/**").permitAll()
                // ===== 可观测性端点 =====
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").access(IpAddressAuthorizationManager.hasIpAddress("127.0.0.1"))
                // ===== 其他所有接口均需认证 =====
                .anyRequest().authenticated())
            // 添加 UserContext 过滤器（双模式：网关 X-User-Id 头优先，无则 fallback 解 JWT）
            .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class)
            // 配置 CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

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
