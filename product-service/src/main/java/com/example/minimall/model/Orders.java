package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体，对应 orders 表，存储用户下单、收货、支付与优惠等订单信息
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

    @TableField(exist = false)
    private List<OrderItem> items;
    @TableField(exist = false)
    private User user;
    @TableField(exist = false)
    private ShippingAddress shippingAddr;
    @TableField(exist = false)
    private Logistics logistics;
    @TableField(exist = false)
    private Payment payment;
}
