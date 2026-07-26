package com.example.minimall.feign;

import com.example.minimall.model.ShippingAddress;
import com.example.minimall.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务 Feign 客户端 — 调用 user-service 的内部 API（/api/internal/**）。
 *
 * <p>用于替代原先直接访问 user / shipping_address 表的本地 Mapper。
 * 返回的 User 对象已在 user-service 侧脱敏（password 为 null）。</p>
 */
@FeignClient(name = "user-service", contextId = "userFeignClient")
public interface UserFeignClient {

    /** 根据 ID 查询用户（脱敏） */
    @GetMapping("/api/internal/user/{id}")
    User getUser(@PathVariable("id") Long id);

    /** 批量查询用户（脱敏） */
    @PostMapping("/api/internal/user/batch")
    List<User> getUsersBatch(@RequestBody List<Long> ids);

    /** 根据地址 ID 查询收货地址 */
    @GetMapping("/api/internal/user/address/{addressId}")
    ShippingAddress getAddress(@PathVariable("addressId") Long addressId);
}
