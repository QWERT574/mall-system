package com.example.minimall.feign;

import com.example.minimall.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端 — 调用 user-service 内部 API 获取用户信息（含 userType）。
 *
 * <p>用于权限拦截器解析当前登录用户的角色（买家/卖家/管理员）。
 * 返回的 User 已在 user-service 侧脱敏（password 为 null）。</p>
 */
@FeignClient(name = "user-service", contextId = "userFeignClient")
public interface UserFeignClient {

    /** 根据 ID 查询用户（脱敏），用于角色鉴权 */
    @GetMapping("/api/internal/user/{id}")
    User getUser(@PathVariable("id") Long id);
}
