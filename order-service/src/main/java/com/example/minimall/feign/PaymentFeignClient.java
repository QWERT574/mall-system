package com.example.minimall.feign;

import com.example.minimall.model.Payment;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 支付服务 Feign 客户端 — 调用 payment-service 的内部 API（/api/internal/**）。
 *
 * <p>用于替代原先直接访问 payment 表的本地 Mapper。</p>
 */
@FeignClient(name = "payment-service", contextId = "paymentFeignClient")
public interface PaymentFeignClient {

    /** 查询指定订单最近一笔支付记录 */
    @GetMapping("/api/internal/payment/order/{orderId}")
    Payment getPaymentByOrderId(@PathVariable("orderId") Long orderId);

    /** 删除指定订单的全部支付记录 */
    @DeleteMapping("/api/internal/payment/order/{orderId}")
    int deletePaymentsByOrderId(@PathVariable("orderId") Long orderId);
}
