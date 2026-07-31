package com.example.minimall.feign;

import com.example.minimall.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务 Feign 客户端 — 调用 user-service 的内部 API（/api/internal/user/**）。
 *
 * <p>用于替代原本地 {@code UserMapper} / {@code UserService} 的跨域直访。
 * 返回的 User 对象已在 user-service 侧脱敏（password 为 null）。</p>
 *
 * <p>主要使用场景：
 * <ul>
 *   <li>{@link com.example.minimall.controller.CustomerServiceController} 查询客服坐席列表（user_type=2 的用户）</li>
 *   <li>{@link com.example.minimall.interceptor.PermissionInterceptor} 根据 userId 解析 userType 做角色鉴权</li>
 * </ul>
 * </p>
 */
@FeignClient(name = "user-service", contextId = "userFeignClient")
public interface UserFeignClient {

    /** 根据 ID 查询用户（脱敏） */
    @GetMapping("/api/internal/user/{id}")
    User getUser(@PathVariable("id") Long id);

    /** 批量查询用户（脱敏） */
    @PostMapping("/api/internal/user/batch")
    List<User> getUsersBatch(@RequestBody List<Long> ids);

    /** 按用户类型查询用户列表（user_type: 0=买家 1=商家 2=管理员） */
    @GetMapping("/api/internal/user/by-type")
    List<User> listByUserType(@RequestParam("userType") Integer userType,
                              @RequestParam(value = "status", required = false) Integer status);

    /** 更新用户状态（启用/禁用，1=启用 0=禁用）— 用户不存在返回 false */
    @PutMapping("/api/internal/user/{id}/status")
    boolean updateUserStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status);
}
