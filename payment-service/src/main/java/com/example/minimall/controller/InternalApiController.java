package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.PaymentMapper;
import com.example.minimall.model.Payment;
import org.springframework.web.bind.annotation.*;

/**
 * 支付服务内部 API — 供其他微服务通过 Feign 调用（不经网关，不走 JWT 认证）。
 *
 * <p>路径前缀：/api/internal/**，在 SecurityConfig 中白名单放行。
 * 返回裸对象（不包 Result），方便 Feign 客户端直接反序列化。</p>
 */
@RestController
@RequestMapping("/api/internal/payment")
public class InternalApiController {

    private final PaymentMapper paymentMapper;

    public InternalApiController(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    /**
     * 查询指定订单最近一笔支付记录（供 order-service 订单详情调用）
     *
     * @param orderId 订单 ID
     * @return 最近一笔支付记录；无记录返回 null
     */
    @GetMapping("/order/{orderId}")
    public Payment getPaymentByOrderId(@PathVariable Long orderId) {
        QueryWrapper<Payment> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId).orderByDesc("created_at").last("LIMIT 1");
        return paymentMapper.selectOne(wrapper);
    }

    /**
     * 删除指定订单的全部支付记录（供 order-service 删除订单调用）
     *
     * @param orderId 订单 ID
     * @return 删除的记录数
     */
    @DeleteMapping("/order/{orderId}")
    public int deletePaymentsByOrderId(@PathVariable Long orderId) {
        QueryWrapper<Payment> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        return paymentMapper.delete(wrapper);
    }
}
