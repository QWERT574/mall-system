package com.example.minimall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.minimall.model.Role;
import com.example.minimall.model.User;

import java.util.List;

/** 用户与角色服务接口 */
public interface UserService {
    /** 根据 openid 查询用户 */
    User findByOpenid(String openid);
    /** 根据手机号查询用户 */
    User findByPhone(String phone);
    /** 根据用户名查询用户 */
    User findByUsername(String username);
    /** 根据 ID 查询用户 */
    User findById(Long id);
    /** 查询所有用户 */
    List<User> findAll();
    /** 分页查询用户 */
    IPage<User> findUsersWithPagination(Integer page, Integer pageSize, String keyword, Integer userType, Integer status);
    /** 保存用户 */
    void save(User user);
    /** 删除用户 */
    void delete(Long id);
    /** 根据角色 ID 查询用户 */
    List<User> findByRoleId(Long roleId);
    /** 查询用户角色 */
    Role findUserRole(Long userId);
    /** 为用户分配角色 */
    void assignRole(Long userId, Long roleId);
    /** 查询所有角色 */
    List<Role> findAllRoles();
    /** 根据 ID 查询角色 */
    Role findRoleById(Long id);
    /** 为用户设置默认角色 */
    void setDefaultRole(Long userId);
    /** 查询所有商家 */
    List<User> findAllSellers();
    /** 分页查询商家 */
    IPage<User> findSellersWithPagination(Integer page, Integer pageSize, String keyword, Integer isVerified, Integer status);
}
