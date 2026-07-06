package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色表 Mapper，对应 role 表
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}