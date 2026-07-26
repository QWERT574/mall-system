package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.ShippingAddressMapper;
import com.example.minimall.mapper.UserMapper;
import com.example.minimall.model.ShippingAddress;
import com.example.minimall.model.User;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 内部 API 控制器 — 供其他微服务通过 Feign 调用获取用户/地址数据。
 * <p>
 * 路径约定：{@code /api/internal/**}（区别于面向前端的 {@code /api/**}）。
 * 在 {@link com.example.minimall.config.SecurityConfig} 中已加入白名单，
 * 服务间直连，不经网关。
 * </p>
 * <p>安全注意：所有返回的 User 对象都会脱敏（password 置空）。</p>
 */
@RestController
@RequestMapping("/api/internal/user")
public class InternalApiController {

    private final UserMapper userMapper;
    private final ShippingAddressMapper shippingAddressMapper;

    public InternalApiController(UserMapper userMapper, ShippingAddressMapper shippingAddressMapper) {
        this.userMapper = userMapper;
        this.shippingAddressMapper = shippingAddressMapper;
    }

    /**
     * 根据 ID 查询用户基本信息（脱敏，不含密码）
     *
     * @param id 用户 ID
     * @return 脱敏后的用户对象；未找到返回 null
     */
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        return desensitize(user);
    }

    /**
     * 批量查询用户基本信息（脱敏）
     *
     * @param ids 用户 ID 列表
     * @return 脱敏后的用户列表
     */
    @PostMapping("/batch")
    public List<User> getUsersBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectBatchIds(ids);
        List<User> result = new ArrayList<>(users.size());
        for (User u : users) {
            result.add(desensitize(u));
        }
        return result;
    }

    /**
     * 查询指定用户的收货地址列表
     *
     * @param id 用户 ID
     * @return 收货地址列表
     */
    @GetMapping("/{id}/addresses")
    public List<ShippingAddress> getAddresses(@PathVariable Long id) {
        QueryWrapper<ShippingAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", id);
        return shippingAddressMapper.selectList(wrapper);
    }

    /**
     * 根据地址 ID 查询单条收货地址（供 order-service 创建订单调用）
     *
     * @param addressId 地址 ID
     * @return 收货地址对象；未找到返回 null
     */
    @GetMapping("/address/{addressId}")
    public ShippingAddress getAddressById(@PathVariable Long addressId) {
        return shippingAddressMapper.selectById(addressId);
    }

    /**
     * 按用户类型查询用户列表（供 chat-service 查询客服坐席列表调用）
     *
     * <p>user_type：0=买家 1=商家 2=管理员/客服</p>
     *
     * @param userType 用户类型
     * @param status   用户状态（可空：1=启用 0=禁用；null 表示不限）
     * @return 脱敏后的用户列表
     */
    @GetMapping("/by-type")
    public List<User> listByUserType(@RequestParam("userType") Integer userType,
                                     @RequestParam(value = "status", required = false) Integer status) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_type", userType);
        if (status != null) {
            wrapper.eq("status", status);
        }
        List<User> users = userMapper.selectList(wrapper);
        List<User> result = new ArrayList<>(users.size());
        for (User u : users) {
            result.add(desensitize(u));
        }
        return result;
    }

    /**
     * 更新用户状态（启用/禁用）— 供 chat-service 客服坐席状态切换调用
     *
     * @param id     用户 ID
     * @param status 新状态：1=启用 0=禁用
     * @return 更新成功返回 true；用户不存在返回 false
     */
    @PutMapping("/{id}/status")
    public boolean updateUserStatus(@PathVariable Long id, @RequestParam("status") Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return false;
        }
        user.setStatus(status);
        return userMapper.updateById(user) > 0;
    }

    /** 脱敏：清除密码字段 */
    private User desensitize(User user) {
        if (user == null) {
            return null;
        }
        user.setPassword(null);
        return user;
    }
}
