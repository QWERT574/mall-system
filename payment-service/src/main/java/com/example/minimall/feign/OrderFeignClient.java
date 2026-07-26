package com.example.minimall.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 订单服务 Feign 客户端 — 调用 order-service 的内部 API（/api/internal/**）。
 *
 * <p>用于替代原先直接访问 orders 表的本地 OrdersMapper。
 * 服务间直连，不经网关，由 Nacos 解析服务名 {@code order-service}。</p>
 *
 * <p><b>事务边界</b>：跨服务写操作（订单状态更新）无法参与 payment-service 本地事务，
 * 采用"先更新订单 → 再写支付记录 → 失败则补偿回滚订单"的模式保证最终一致性。</p>
 */
@FeignClient(name = "order-service", contextId = "orderFeignClient")
public interface OrderFeignClient {

    /**
     * 根据 ID 查询订单（仅需支付相关字段，由 {@link OrderDto} 接收）。
     *
     * @param id 订单 ID
     * @return 订单 DTO；未找到返回 null
     */
    @GetMapping("/api/internal/order/{id}")
    OrderDto getOrder(@PathVariable("id") Long id);

    /**
     * 条件更新订单为已支付（仅当 status=0 待支付时生效，原子操作防并发重复支付）。
     *
     * @param id 订单 ID
     * @return true=更新成功；false=订单不存在/已被支付/并发冲突
     */
    @PutMapping("/api/internal/order/{id}/pay")
    boolean updateOrderToPaid(@PathVariable("id") Long id);

    /**
     * 条件回滚订单支付状态（已支付 → 待支付）。
     * <p>补偿端点：支付记录写入失败时调用，回滚订单状态以保证一致性。</p>
     *
     * @param id 订单 ID
     * @return true=回滚成功；false=订单不存在/非已支付状态/并发冲突
     */
    @PutMapping("/api/internal/order/{id}/revert-pay")
    boolean revertOrderPay(@PathVariable("id") Long id);
}
