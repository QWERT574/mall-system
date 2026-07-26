package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体 — chat-service 侧的轻量 Feign DTO。
 *
 * <p><b>重构说明</b>：原 Orders 实体包含对 OrderItem/ShippingAddress/Logistics/Payment
 * 等子对象的关联引用（@TableField(exist=false)），微服务领域裁剪后这些跨域 Model
 * 已不在 chat-service 内保留。此处仅保留 order-service 的 InternalApiController
 * 通过 Feign 返回的 JSON 能够反序列化的标量字段。</p>
 *
 * <p>chat-service 仅在 {@code AdminInterventionServiceImpl.enrichInterventionWithDetails}
 * 中使用 {@link #getOrderSn()} 与 {@link #getTotalPrice()} 两个字段。</p>
 */
@Data
@TableName("orders")
public class Orders {
    @TableId
    private Long id;
    @TableField("order_no")
    private String orderSn;
    private Long userId;
    private Long shippingAddressId;
    private String consignee;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Long logisticsId;
    @TableField("total_amount")
    private BigDecimal totalPrice;
    @TableField("pay_amount")
    private BigDecimal payAmount;
    private Integer status;
    @TableField("pay_status")
    private Integer payStatus;
    private String remark;
    @TableField("created_at")
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userCouponId;
    @TableField("discount_amount")
    private BigDecimal discountAmount;
}

