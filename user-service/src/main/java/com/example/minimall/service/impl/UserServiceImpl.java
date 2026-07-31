package com.example.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.mapper.RoleMapper;
import com.example.minimall.mapper.UserMapper;
import com.example.minimall.mapper.UserRoleMapper;
import com.example.minimall.model.Role;
import com.example.minimall.model.User;
import com.example.minimall.model.UserRole;
import com.example.minimall.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 用户与角色服务实现 */
@Service
public class UserServiceImpl implements UserService {
    /** 用户Mapper */
    private final UserMapper userMapper;
    /** 角色Mapper */
    private final RoleMapper roleMapper;
    /** 用户角色关联Mapper */
    private final UserRoleMapper userRoleMapper;

    public UserServiceImpl(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    /** 根据 openid 查询用户 */
    public User findByOpenid(String openid) {
        return userMapper.selectByOpenid(openid);
    }

    @Override
    /** 根据手机号查询用户 */
    public User findByPhone(String phone) {
        return userMapper.selectByPhone(phone);
    }

    @Override
    /** 根据用户名查询用户 */
    public User findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    /** 根据 id 查询用户 */
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    /** 查询所有用户（**不做任何过滤**） */
    public List<User> findAll() {
        return userMapper.selectList(null);
    }

    @Override
    /**
     * 分页查询用户（管理后台用）
     * <p>
     * 支持关键词（昵称/用户名/手机号模糊匹配）、用户类型、状态的多条件筛选。
     * </p>
     *
     * @param page     页码（从 1 开始）
     * @param pageSize 每页大小
     * @param keyword  关键词（可空）
     * @param userType 用户类型：0-普通 1-商家 2-管理员（可空）
     * @param status   账号状态：0-禁用 1-启用（实际映射到 is_verified 字段，可空）
     * @return 用户分页
     */
    public IPage<User> findUsersWithPagination(Integer page, Integer pageSize, String keyword, Integer userType, Integer status) {
        // 创建分页对象
        Page<User> pageObj = new Page<>(page, pageSize);
        
        // 创建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        
        // 添加关键词筛选
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like("nickname", keyword).or().like("username", keyword).or().like("phone", keyword);
        }
        
        // 添加用户类型筛选
        if (userType != null) {
            queryWrapper.eq("user_type", userType);
        }
        
        // 添加状态筛选
        if (status != null) {
            queryWrapper.eq("is_verified", status);
        }
        
        // 执行分页查询
        return userMapper.selectPage(pageObj, queryWrapper);
    }

    @Override
    /** 保存用户（**根据 ID 是否为空判断**新增或更新） */
    public void save(User user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
    }

    @Override
    /** 删除用户（**真实删除**） */
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    /** 根据角色 ID 查询拥有该角色的用户列表 */
    public List<User> findByRoleId(Long roleId) {
        return userMapper.selectByRoleId(roleId);
    }

    @Override
    /**
     * 查询用户的角色（**单角色模型**）
     * <p>一个用户当前只对应一个角色</p>
     *
     * @param userId 用户 ID
     * @return 角色实体
     */
    public Role findUserRole(Long userId) {
        return userMapper.selectUserRole(userId);
    }

    @Override
    /**
     * 为用户分配角色（**先清空再插入**，覆盖式）
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     */
    public void assignRole(Long userId, Long roleId) {
        // 先删除现有角色
        userRoleMapper.deleteByUserId(userId);
        // 分配新角色
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }

    @Override
    /** 查询所有角色 */
    public List<Role> findAllRoles() {
        return roleMapper.selectList(null);
    }

    @Override
    /** 根据 ID 查询角色 */
    public Role findRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    /**
     * 为新用户设置默认角色（roleId=1 假定为"普通用户"角色）
     *
     * @param userId 用户 ID
     */
    public void setDefaultRole(Long userId) {
        // 查询普通用户角色（假设 roleId=1 是普通用户）
        Role defaultRole = roleMapper.selectById(1L);
        if (defaultRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(defaultRole.getId());
            userRoleMapper.insert(userRole);
        }
    }

    @Override
    /** 查询所有商家（**user_type=1**） */
    public List<User> findAllSellers() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_type", 1); // 1 表示商家
        return userMapper.selectList(queryWrapper);
    }

    @Override
    /**
     * 分页查询商家（商家管理用）
     * <p>
     * 只查 user_type=1 的用户，支持关键词（昵称/用户名/手机号/公司名）和审核状态筛选，
     * 按注册时间倒序。
     * </p>
     *
     * @param page        页码
     * @param pageSize    每页大小
     * @param keyword     关键词（可空）
     * @param isVerified  审核状态：0-未审核 1-已审核（可空）
     * @param status      账号状态：0-禁用 1-启用（可空）
     * @return 商家分页
     */
    public IPage<User> findSellersWithPagination(
            Integer page, Integer pageSize, String keyword, Integer isVerified, Integer status) {
        // 创建分页对象
        Page<User> pageObj = 
            new Page<>(page, pageSize);
        
        // 创建查询条件
        QueryWrapper<User> queryWrapper = 
            new QueryWrapper<>();
        
        // 只查询商家
        queryWrapper.eq("user_type", 1);
        
        // 添加关键词筛选
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(qw -> qw.like("nickname", keyword)
                .or()
                .like("username", keyword)
                .or()
                .like("phone", keyword)
                .or()
                .like("company_name", keyword));
        }
        
        // 添加审核状态筛选
        if (isVerified != null) {
            queryWrapper.eq("is_verified", isVerified);
        }
        
        // 添加账号状态筛选
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        // 按注册时间倒序
        queryWrapper.orderByDesc("created_at");
        
        // 执行分页查询
        return userMapper.selectPage(pageObj, queryWrapper);
    }
}
