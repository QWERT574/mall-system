package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.LogisticsTrace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 物流轨迹表 Mapper，对应 logistics_trace 表
 */
@Mapper
public interface LogisticsTraceMapper extends BaseMapper<LogisticsTrace> {
    /** 根据物流 ID 查询轨迹列表 */
    List<LogisticsTrace> selectByLogisticsId(@Param("logisticsId") Long logisticsId);
    /** 根据物流 ID 删除轨迹 */
    int deleteByLogisticsId(@Param("logisticsId") Long logisticsId);
}