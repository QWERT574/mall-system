package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单表 Mapper，对应 orders 表
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
    /** 根据用户 ID 查询订单列表 */
    List<Orders> selectByUserId(@Param("userId") Long userId);
    /** 查询订单及其明细项 */
    Orders selectOrderWithItems(@Param("id") Long id);
}