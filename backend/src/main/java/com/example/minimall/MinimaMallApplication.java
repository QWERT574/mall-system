package com.example.minimall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商城系统启动入口。
 *
 * <p>开启异步任务 {@link EnableAsync}、定时任务 {@link EnableScheduling}、缓存 {@link EnableCaching}，
 * 并扫描 {@code com.example.minimall.mapper} 包下的 MyBatis 映射接口。
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
@MapperScan("com.example.minimall.mapper")
public class MinimaMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinimaMallApplication.class, args);
    }
}