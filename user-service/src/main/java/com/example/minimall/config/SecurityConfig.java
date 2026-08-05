package com.example.minimall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
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
    private final com.example.minimall.filter.XssFilter xssFilter;

    /**
     * 构造方法 — 网关已做 JWT 鉴权，下游只读 X-User-Id Header。
     */
    public SecurityConfig(UserContextFilter userContextFilter,
                          com.example.minimall.filter.XssFilter xssFilter) {
        this.userContextFilter = userContextFilter;
        this.xssFilter = xssFilter;
    }

    /**
     * 装配 BCrypt 密码编码器，用于用户密码加密与校验。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 装配安全过滤链：禁用 CSRF 与 Session、配置白名单与认证策略、注册 JWT 过滤器与 CORS。
     * <p>
     * Spring Security 6 迁移说明（Boot 3 内置 Security 6.x）：
     * 1. 链式 API 全面改为 Lambda 风格：{@code .csrf().disable()} 已删除，必须写成
     *    {@code http.csrf(AbstractHttpConfigurer::disable)}；
     * 2. {@code authorizeRequests()/antMatchers()} 已删除，改为
     *    {@code authorizeHttpRequests(auth -> auth.requestMatchers(...))}；
     * 3. {@code .and()} 拼接不再需要，每个配置器独立成段。
     * 行为与迁移前完全一致。
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（无状态 JWT 方案，无 Cookie 会话，天然免疫 CSRF）
            .csrf(AbstractHttpConfigurer::disable)
            // 禁用 Session：每次请求独立认证
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置授权规则（修复后：仅放行真正公开的接口）
            .authorizeHttpRequests(auth -> auth
                // ===== 内部 API（Feign 服务间调用，不经网关）=====
                .requestMatchers("/api/internal/**").permitAll()
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
                // ===== 商家公开信息（商品详情页展示，与网关白名单一致，允许匿名）=====
                .requestMatchers("/api/seller/public/**").permitAll()
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
                // ===== 可观测性端点 (健康/指标/Prometheus) =====
                // 生产环境建议仅内网/网关放行 /actuator/prometheus, 此处全部放行便于本地与 K8s 探针
                .requestMatchers("/actuator/health").permitAll()
                // Security 6 已移除 AuthorizedUrl.hasIpAddress()，改用表达式授权管理器等价实现
                .requestMatchers("/actuator/**").access(new WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1')"))
                // ===== 其他所有接口均需认证 =====
                // 业务级角色控制（买家/卖家/管理员）由 PermissionInterceptor 进一步校验
                .anyRequest().authenticated()
            )
            // 添加 JWT 过滤器（顺序：XSS 在前，用户上下文在后）
            .addFilterBefore(xssFilter, UsernamePasswordAuthenticationFilter.class)
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
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
