package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户与角色关联表 Mapper，对应 user_role 表
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /** 根据用户 ID 删除其所有角色关联 */
    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    /** 查询用户关联的角色 ID 列表 */
    @Select("SELECT role_id FROM user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
