package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色与权限关联实体，对应 role_permission 表，存储角色与权限的多对多关系
 */
@Data
@TableName("role_permission")
public class RolePermission {
    private Long roleId;
    private Long permissionId;
}
