package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限表 Mapper，对应 permission 表
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
