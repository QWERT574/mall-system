package com.example.minimall.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 售后申请请求体。
 *
 * <p>字段含义：orderId 订单、userId 申请人、productId 售后商品、serviceType 服务类型、reason 售后原因。
 */
public class AfterSaleRequest {
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    
    @NotNull(message = "服务类型不能为空")
    private Integer serviceType;
    
    @NotBlank(message = "售后原因不能为空")
    private String reason;
}
