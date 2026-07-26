package com.example.minimall.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.annotation.DbType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类。
 * <p>
 * 装配 MyBatis-Plus 的核心拦截器链，目前主要注册 MySQL 分页拦截器，
 * 限制单次最大查询条数并关闭溢出 total 后的查询。
 * </p>
 */
@Configuration
public class MyBatisPlusConfig {
    /**
     * 装配 MyBatis-Plus 拦截器：注册 MySQL 分页插件，最大单页 1000 条。
     *
     * @return 配置完成的 MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setMaxLimit(1000L);
        paginationInnerInterceptor.setOverflow(false);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}