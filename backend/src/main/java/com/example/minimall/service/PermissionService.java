package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.PermissionMapper;
import com.example.minimall.mapper.RolePermissionMapper;
import com.example.minimall.model.Permission;
import com.example.minimall.model.RolePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 权限与角色权限关联服务 */
@Service
public class PermissionService {

    /** 权限 Mapper */
    private final PermissionMapper permissionMapper;
    /** 角色权限关联 Mapper */
    private final RolePermissionMapper rolePermissionMapper;

    public PermissionService(PermissionMapper permissionMapper, RolePermissionMapper rolePermissionMapper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /** 获取所有权限（**不做任何过滤**） */
    public List<Permission> listAll() {
        return permissionMapper.selectList(null);
    }

    /**
     * 查询父权限下的子权限列表
     * <p>parentId 为 null 时查询顶级权限（parent_id IS NULL）</p>
     *
     * @param parentId 父权限 ID
     * @return 子权限列表
     */
    public List<Permission> findByParentId(Long parentId) {
        QueryWrapper<Permission> qw = new QueryWrapper<>();
        if (parentId == null) {
            qw.isNull("parent_id");
        } else {
            qw.eq("parent_id", parentId);
        }
        return permissionMapper.selectList(qw);
    }

    /**
     * 根据权限 ID 查询
     *
     * @param id 权限主键
     * @return 权限实体
     * @throws IllegalArgumentException id 为空时
     */
    public Permission findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("权限ID不能为空");
        }
        return permissionMapper.selectById(id);
    }

    /**
     * 新增或更新权限（**事务**）
     * <p>校验 name / code 必填</p>
     *
     * @param permission 权限实体
     * @return 保存后的实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Permission save(Permission permission) {
        if (permission.getName() == null || permission.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("权限名称不能为空");
        }
        if (permission.getCode() == null || permission.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("权限编码不能为空");
        }
        if (permission.getId() == null) {
            permissionMapper.insert(permission);
        } else {
            permissionMapper.updateById(permission);
        }
        return permission;
    }

    /**
     * 删除权限（**事务**）
     * <p>先删除角色权限关联记录，再删除权限本身</p>
     *
     * @param id 权限主键
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("权限ID不能为空");
        }
        QueryWrapper<RolePermission> qw = new QueryWrapper<>();
        qw.eq("permission_id", id);
        rolePermissionMapper.delete(qw);
        permissionMapper.deleteById(id);
    }

    /**
     * 查询角色拥有的权限列表
     *
     * @param roleId 角色 ID
     * @return 权限列表（无权限时返回空集合）
     */
    public List<Permission> findByRoleId(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        List<Long> permissionIds = rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        return permissionMapper.selectBatchIds(permissionIds);
    }

    /**
     * 为角色**重新**分配权限列表（**事务**）
     * <p>采用"先清空再插入"的策略，等同于覆盖</p>
     *
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 集合（为空或 null 表示清空该角色所有权限）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        if (roleId == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        QueryWrapper<RolePermission> qw = new QueryWrapper<>();
        qw.eq("role_id", roleId);
        rolePermissionMapper.delete(qw);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                rolePermissionMapper.insertRolePermission(roleId, permissionId);
            }
        }
    }
}
