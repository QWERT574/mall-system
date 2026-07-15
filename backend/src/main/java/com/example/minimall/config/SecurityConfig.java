package com.example.minimall.config;

import com.example.minimall.utils.JwtUtil;
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

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 构造方法，注入 JWT 工具类并组装 JWT 认证过滤器。
     */
    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil);
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
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF
            .csrf().disable()
            // 禁用 Session
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 配置授权规则（修复后：仅放行真正公开的接口）
            .authorizeRequests()
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
                // ===== 文件上传/下载 + 调试 + 静态资源 =====
                .antMatchers("/api/upload/**").permitAll()
                .antMatchers("/api/debug/**").permitAll()
                .antMatchers("/uploads/**").permitAll()
                .antMatchers("/images/**").permitAll()
                // ===== WebSocket 端点 =====
                .antMatchers("/ws-chat/**").permitAll()
                // ===== 可观测性端点 (健康/指标/Prometheus) =====
                // 生产环境建议仅内网/网关放行 /actuator/prometheus, 此处全部放行便于本地与 K8s 探针
                .antMatchers("/actuator/**").permitAll()
                // ===== 其他所有接口均需认证 =====
                // 业务级角色控制（买家/卖家/管理员）由 PermissionInterceptor 进一步校验
                .anyRequest().authenticated()
            .and()
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 配置 CORS
            .cors().configurationSource(corsConfigurationSource());

        return http.build();
    }
    
    /**
     * 装配 CORS 配置源：放行本地端口、内网网段，支持凭据与常用 HTTP 方法。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://192.168.*:*",
            "https://*.github.io",
            "https://*.onrender.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
