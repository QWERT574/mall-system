package com.example.minimall.feign;

import com.example.minimall.model.AfterSaleService;
import com.example.minimall.model.Orders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 订单服务 Feign 客户端 — 调用 order-service 的内部 API（/api/internal/**）。
 *
 * <p>用于替代原本地 {@code OrdersMapper} / {@code AfterSaleServiceMapper} 的跨域直访。</p>
 *
 * <p>主要使用场景：
 * {@link com.example.minimall.service.impl.AdminInterventionServiceImpl}
 * 在 enrich 介入申请详情时，根据 orderId 查询订单号/金额，以及关联的售后单。</p>
 */
@FeignClient(name = "order-service", contextId = "orderFeignClient")
public interface OrderFeignClient {

    /** 根据 ID 查询订单 */
    @GetMapping("/api/internal/order/{id}")
    Orders getOrder(@PathVariable("id") Long id);

    /** 根据订单 ID 查询售后记录列表 */
    @GetMapping("/api/internal/aftersale/{orderId}")
    List<AfterSaleService> getAfterSaleByOrderId(@PathVariable("orderId") Long orderId);
}
