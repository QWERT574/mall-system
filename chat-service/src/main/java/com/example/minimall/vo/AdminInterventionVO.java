package com.example.minimall.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员介入工单视图对象：聚合订单、买家、卖家、商品、售后等信息，
 * 用于平台客服后台查看与处理纠纷/投诉。
 */
@Data
public class AdminInterventionVO {
    private Long id;
    private Long orderId;
    private Long productId;
    private Long sellerId;
    private Long userId;
    private Long sessionId;
    private Long afterSaleId;
    private String issueType;
    private String title;
    private String description;
    private String evidenceImages;
    private Integer status;
    private Long adminId;
    private String result;
    private String adminRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;
    private String buyerName;
    private String buyerPhone;
    private String sellerName;
    private String sellerShopName;
    private String orderNo;
    private Double orderAmount;
    private String productName;
    private Integer serviceType;
    private Double amount;
    private LocalDateTime interventionAt;
}
