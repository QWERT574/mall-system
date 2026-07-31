package com.example.minimall.feign;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单轻量 DTO — payment-service 通过 Feign 调用 order-service 内部 API 时接收订单数据。
 *
 * <p>仅包含支付流程所需字段（userId 鉴权、status 状态校验、totalPrice 支付金额），
 * 避免在 payment-service 引入 order-service 的完整 Orders Model，保持服务领域隔离。
 * Jackson 按 字段名 匹配反序列化，order-service 返回的多余字段自动忽略。</p>
 */
@Data
public class OrderDto {
    /** 订单 ID */
    private Long id;
    /** 下单用户 ID（用于支付鉴权：仅订单归属人可支付） */
    private Long userId;
    /** 订单状态：0=待支付，1=已支付，... */
    private Integer status;
    /** 订单总金额（支付记录写入金额） */
    private BigDecimal totalPrice;
}
