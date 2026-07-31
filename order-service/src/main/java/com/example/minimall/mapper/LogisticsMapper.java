package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Logistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 物流信息表 Mapper，对应 logistics 表
 */
@Mapper
public interface LogisticsMapper extends BaseMapper<Logistics> {
    /** 根据订单 ID 查询物流信息 */
    Logistics selectByOrderId(@Param("orderId") Long orderId);
}
