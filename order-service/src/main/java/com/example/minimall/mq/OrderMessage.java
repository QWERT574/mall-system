package com.example.minimall.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * RocketMQ 订单消息体 — 下单成功后投递到 MQ。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private Long couponId;              // 核销的优惠券 ID
    private List<OrderItemMsg> items;   // 订单商品列表

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemMsg implements Serializable {
        private Long productId;
        private Integer quantity;
    }
}
