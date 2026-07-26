package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.AfterSaleServiceMapper;
import com.example.minimall.mapper.LogisticsMapper;
import com.example.minimall.mapper.LogisticsTraceMapper;
import com.example.minimall.mapper.OrdersMapper;
import com.example.minimall.model.AfterSaleService;
import com.example.minimall.model.Logistics;
import com.example.minimall.model.LogisticsTrace;
import com.example.minimall.model.Orders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单服务内部 API — 供其他微服务通过 Feign 调用（不经网关，不走 JWT 认证）。
 *
 * <p>路径前缀：/api/internal/**，在 SecurityConfig 中白名单放行。
 * 返回裸对象（不包 Result），方便 Feign 客户端直接反序列化。</p>
 */
@RestController
@RequestMapping("/api/internal")
public class InternalApiController {

    private static final Logger logger = LoggerFactory.getLogger(InternalApiController.class);

    private final OrdersMapper ordersMapper;
    private final LogisticsMapper logisticsMapper;
    private final LogisticsTraceMapper logisticsTraceMapper;
    private final AfterSaleServiceMapper afterSaleServiceMapper;

    public InternalApiController(OrdersMapper ordersMapper,
                                 LogisticsMapper logisticsMapper,
                                 LogisticsTraceMapper logisticsTraceMapper,
                                 AfterSaleServiceMapper afterSaleServiceMapper) {
        this.ordersMapper = ordersMapper;
        this.logisticsMapper = logisticsMapper;
        this.logisticsTraceMapper = logisticsTraceMapper;
        this.afterSaleServiceMapper = afterSaleServiceMapper;
    }

    // ======================== 订单 ========================

    /**
     * 根据 ID 查询订单（供 payment-service / ai-service 调用）
     *
     * @param id 订单 ID
     * @return 订单实体；未找到返回 null
     */
    @GetMapping("/order/{id}")
    public Orders getOrder(@PathVariable Long id) {
        return ordersMapper.selectById(id);
    }

    /**
     * 条件更新订单为已支付（仅当 status=0 待支付时生效）
     * <p>原子操作，防并发重复支付。供 payment-service 支付成功后调用。</p>
     *
     * @param id 订单 ID
     * @return true=更新成功；false=订单不存在/已被支付/并发冲突
     */
    @PutMapping("/order/{id}/pay")
    public boolean updateOrderToPaid(@PathVariable Long id) {
        logger.info("Internal: updateOrderToPaid orderId={}", id);
        return ordersMapper.updateToPaidIfPending(id) > 0;
    }

    /**
     * 条件回滚订单支付状态（已支付 status=1 → 待支付 status=0）。
     * <p>补偿端点：供 payment-service 在支付记录写入失败时回滚订单状态，
     * 避免"订单已支付但无支付记录"的数据不一致。</p>
     *
     * @param id 订单 ID
     * @return true=回滚成功；false=订单不存在 / 非已支付状态 / 并发冲突
     */
    @PutMapping("/order/{id}/revert-pay")
    public boolean revertOrderPay(@PathVariable Long id) {
        logger.warn("Internal: revertOrderPay orderId={} (compensation)", id);
        return ordersMapper.revertToPendingIfPaid(id) > 0;
    }

    /**
     * 根据用户 ID 查询订单列表（供 ai-service 物流查询调用）。
     *
     * @param userId 用户 ID
     * @return 该用户的订单列表
     */
    @GetMapping("/order/user/{userId}")
    public List<Orders> getOrdersByUserId(@PathVariable Long userId) {
        return ordersMapper.selectByUserId(userId);
    }

    // ======================== 物流 ========================

    /**
     * 根据订单 ID 查询物流信息（供 ai-service 调用）
     *
     * @param orderId 订单 ID
     * @return 物流记录列表
     */
    @GetMapping("/logistics/{orderId}")
    public List<Logistics> getLogisticsByOrderId(@PathVariable Long orderId) {
        QueryWrapper<Logistics> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        return logisticsMapper.selectList(wrapper);
    }

    /**
     * 根据物流 ID 查询物流信息（供 ai-service 物流查询调用）。
     *
     * @param id 物流记录主键 ID
     * @return 物流实体；未找到返回 null
     */
    @GetMapping("/logistics/by-id/{id}")
    public Logistics getLogisticsById(@PathVariable Long id) {
        return logisticsMapper.selectById(id);
    }

    /**
     * 根据物流 ID 查询物流轨迹（供 ai-service 物流查询调用）。
     *
     * @param logisticsId 物流记录 ID
     * @return 物流轨迹列表（按时间倒序）
     */
    @GetMapping("/logistics/trace/{logisticsId}")
    public List<LogisticsTrace> getLogisticsTraces(@PathVariable Long logisticsId) {
        return logisticsTraceMapper.selectByLogisticsId(logisticsId);
    }

    // ======================== 售后 ========================

    /**
     * 根据订单 ID 查询售后记录（供 ai-service 调用）
     *
     * @param orderId 订单 ID
     * @return 售后记录列表
     */
    @GetMapping("/aftersale/{orderId}")
    public List<AfterSaleService> getAfterSaleByOrderId(@PathVariable Long orderId) {
        QueryWrapper<AfterSaleService> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        return afterSaleServiceMapper.selectList(wrapper);
    }

    /**
     * 根据用户 ID 查询售后记录（供 ai-service 售后咨询调用）。
     *
     * @param userId 用户 ID
     * @return 该用户的售后记录列表
     */
    @GetMapping("/aftersale/user/{userId}")
    public List<AfterSaleService> getAfterSalesByUserId(@PathVariable Long userId) {
        QueryWrapper<AfterSaleService> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return afterSaleServiceMapper.selectList(wrapper);
    }
}
