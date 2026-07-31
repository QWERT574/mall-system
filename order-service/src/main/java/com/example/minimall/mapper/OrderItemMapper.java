package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细表 Mapper，对应 order_item 表
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
    /** 根据订单 ID 查询订单明细列表 */
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);
}