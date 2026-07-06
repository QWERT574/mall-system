package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Role;
import com.example.minimall.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户表 Mapper，对应 user 表
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /** 根据 openid 查询用户 */
    User selectByOpenid(@Param("openid") String openid);

    /** 根据手机号查询用户 */
    User selectByPhone(@Param("phone") String phone);

    /** 根据用户名查询用户 */
    User selectByUsername(@Param("username") String username);

    /** 根据角色 ID 查询用户列表 */
    List<User> selectByRoleId(@Param("roleId") Long roleId);

    /** 查询用户所属角色 */
    Role selectUserRole(@Param("userId") Long userId);
}