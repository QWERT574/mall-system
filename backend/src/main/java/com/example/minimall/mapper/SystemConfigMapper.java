package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置表 Mapper，对应 system_config 表
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /** 根据配置键查询配置值 */
    @Select("SELECT config_value FROM system_config WHERE config_key = #{configKey}")
    String getConfigValue(String configKey);
}
