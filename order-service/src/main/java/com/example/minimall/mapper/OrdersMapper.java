package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 条件更新：仅当订单为待支付（status=0）时改为已支付（status=1、pay_status=1）。
     * <p>原子操作，防并发重复支付。供内部 API（payment-service Feign 调用）使用。</p>
     *
     * @param id 订单 ID
     * @return 受影响行数：1=更新成功；0=订单不存在 / 已被支付 / 并发冲突
     */
    @Update("UPDATE orders SET status = 1, pay_status = 1 WHERE id = #{id} AND status = 0")
    int updateToPaidIfPending(@Param("id") Long id);

    /**
     * 条件回滚：仅当订单为已支付（status=1）时改回待支付（status=0、pay_status=0）。
     * <p>补偿操作，供 payment-service 支付记录写入失败时回滚订单状态，避免"订单已支付但无支付记录"的数据不一致。</p>
     *
     * @param id 订单 ID
     * @return 受影响行数：1=回滚成功；0=订单不存在 / 非已支付状态 / 并发冲突
     */
    @Update("UPDATE orders SET status = 0, pay_status = 0 WHERE id = #{id} AND status = 1")
    int revertToPendingIfPaid(@Param("id") Long id);
}