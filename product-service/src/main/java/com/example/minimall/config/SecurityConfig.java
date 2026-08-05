package com.example.minimall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.IpAddressAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserContextFilter userContextFilter;

    public SecurityConfig(UserContextFilter userContextFilter) {
        this.userContextFilter = userContextFilter;
    }

    /**
     * Spring Security 6 迁移说明（Boot 3 内置 Security 6.x）：
     * 1. 链式 API 全面改为 Lambda 风格：{@code .csrf().disable()} 已删除，必须写成
     *    {@code http.csrf(AbstractHttpConfigurer::disable)}；
     * 2. {@code authorizeRequests()/antMatchers()} 已删除，改为
     *    {@code authorizeHttpRequests(auth -> auth.requestMatchers(...))}；
     * 3. Security 6.4 已移除 AuthorizedUrl.hasIpAddress()，改用
     *    {@code IpAddressAuthorizationManager.hasIpAddress(...)}（构造器非 public，必须用静态工厂）。
     * 行为与迁移前完全一致。
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ===== 内部 API（Feign 服务间调用，不经网关）=====
                .requestMatchers("/api/internal/**").permitAll()
                // ===== 商品浏览公开（仅 GET，防止匿名增删改）=====
                .requestMatchers(HttpMethod.GET, "/api/product/list", "/api/product/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/product/category/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/product/recommended", "/api/product/hot").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/product/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/product/*/specs").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/product/detail/**").permitAll()
                // ===== 分类公开（仅 GET）=====
                .requestMatchers(HttpMethod.GET, "/api/category/**").permitAll()
                // ===== 活动/优惠券/折扣 公开浏览（仅 GET）=====
                .requestMatchers(HttpMethod.GET, "/api/activity/list", "/api/activity/recommended").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/activity/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/coupon/available", "/api/coupon/list").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/coupon/calculate/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/discount/list", "/api/discount/active").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/discount/active-with-products").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/discount/*").permitAll()
                // ===== 评价公开浏览（仅 GET）=====
                .requestMatchers(HttpMethod.GET, "/api/review/list", "/api/review/product/**").permitAll()
                // 静态资源公开（商品图片/Banner，仅 GET）
                .requestMatchers(HttpMethod.GET, "/uploads/**", "/images/**").permitAll()
                // 健康检查
                .requestMatchers("/actuator/health").permitAll()
                // Security 6.4 已移除 hasIpAddress()，改用 IpAddressAuthorizationManager 静态工厂
                .requestMatchers("/actuator/**").access(IpAddressAuthorizationManager.hasIpAddress("127.0.0.1"))
                // 其余需要认证
                .anyRequest().authenticated()
            )
            .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
