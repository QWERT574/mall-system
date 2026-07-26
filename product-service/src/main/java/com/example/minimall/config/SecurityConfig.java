package com.example.minimall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            // ===== 内部 API（Feign 服务间调用，不经网关）=====
            .antMatchers("/api/internal/**").permitAll()
            // ===== 商品浏览公开（仅 GET，防止匿名增删改）=====
            .antMatchers(HttpMethod.GET, "/api/product/list", "/api/product/search").permitAll()
            .antMatchers(HttpMethod.GET, "/api/product/category/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/product/recommended", "/api/product/hot").permitAll()
            .antMatchers(HttpMethod.GET, "/api/product/*").permitAll()
            .antMatchers(HttpMethod.GET, "/api/product/*/specs").permitAll()
            .antMatchers(HttpMethod.GET, "/api/product/detail/**").permitAll()
            // ===== 分类公开（仅 GET）=====
            .antMatchers(HttpMethod.GET, "/api/category/**").permitAll()
            // ===== 活动/优惠券/折扣 公开浏览（仅 GET）=====
            .antMatchers(HttpMethod.GET, "/api/activity/list", "/api/activity/recommended").permitAll()
            .antMatchers(HttpMethod.GET, "/api/activity/*").permitAll()
            .antMatchers(HttpMethod.GET, "/api/coupon/available", "/api/coupon/list").permitAll()
            .antMatchers(HttpMethod.GET, "/api/coupon/calculate/*").permitAll()
            .antMatchers(HttpMethod.GET, "/api/discount/list", "/api/discount/active").permitAll()
            .antMatchers(HttpMethod.GET, "/api/discount/active-with-products").permitAll()
            .antMatchers(HttpMethod.GET, "/api/discount/*").permitAll()
            // ===== 评价公开浏览（仅 GET）=====
            .antMatchers(HttpMethod.GET, "/api/review/list", "/api/review/product/**").permitAll()
            // 静态资源公开（商品图片/Banner，仅 GET）
            .antMatchers(HttpMethod.GET, "/uploads/**", "/images/**").permitAll()
            // 健康检查
            .antMatchers("/actuator/health").permitAll()
            .antMatchers("/actuator/**").hasIpAddress("127.0.0.1")
            // 其余需要认证
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
