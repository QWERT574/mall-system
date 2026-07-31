package com.example.minimall.feign;

import com.example.minimall.model.AfterSaleService;
import com.example.minimall.model.Logistics;
import com.example.minimall.model.LogisticsTrace;
import com.example.minimall.model.Orders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 订单服务 Feign 客户端 — 调用 order-service 的内部 API（/api/internal/**）。
 *
 * <p>用于替代原先直接访问 orders / logistics / logistics_trace / after_sale_service 表的本地 Service。
 * 服务间直连，不经网关，由 Nacos 解析服务名 {@code order-service}。</p>
 *
 * <p><b>返回类型说明</b>：返回 ai-service 本地的 {@link Orders} / {@link Logistics} /
 * {@link LogisticsTrace} / {@link AfterSaleService} 模型（作为 DTO 使用），
 * Jackson 按 字段名 匹配反序列化 order-service 返回的 JSON。这些模型在 ai-service 中不再有对应 Mapper，
 * 仅为承载 Feign 响应数据的纯 POJO。</p>
 */
@FeignClient(name = "order-service", contextId = "aiOrderFeignClient")
public interface OrderFeignClient {

    // ======================== 订单 ========================

    /** 根据用户 ID 查询订单列表（替代原 OrderService.findByUserId） */
    @GetMapping("/api/internal/order/user/{userId}")
    List<Orders> getOrdersByUserId(@PathVariable("userId") Long userId);

    // ======================== 物流 ========================

    /** 根据物流 ID 查询物流信息（替代原 LogisticsService.findById） */
    @GetMapping("/api/internal/logistics/by-id/{id}")
    Logistics getLogisticsById(@PathVariable("id") Long id);

    /** 根据物流 ID 查询物流轨迹（替代原 LogisticsService.findTraceByLogisticsId） */
    @GetMapping("/api/internal/logistics/trace/{logisticsId}")
    List<LogisticsTrace> getLogisticsTraces(@PathVariable("logisticsId") Long logisticsId);

    // ======================== 售后 ========================

    /** 根据用户 ID 查询售后记录（替代原 AfterSaleServiceApi.getAfterSalesByUserId） */
    @GetMapping("/api/internal/aftersale/user/{userId}")
    List<AfterSaleService> getAfterSalesByUserId(@PathVariable("userId") Long userId);
}
