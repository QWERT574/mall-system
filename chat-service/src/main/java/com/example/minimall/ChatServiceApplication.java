package com.example.minimall;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
@SpringBootApplication @EnableDiscoveryClient @EnableFeignClients
@MapperScan("com.example.minimall.mapper")
public class ChatServiceApplication {
    public static void main(String[] args) { SpringApplication.run(ChatServiceApplication.class, args); }
}
