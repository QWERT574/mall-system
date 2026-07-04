package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.AdminIntervention;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员介入记录 Mapper，对应 admin_intervention 表
 */
@Mapper
public interface AdminInterventionMapper extends BaseMapper<AdminIntervention> {
}
