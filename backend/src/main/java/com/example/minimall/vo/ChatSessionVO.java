package com.example.minimall.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天会话视图对象：聚合用户、商家、客服、商品、订单及未读/在线状态等会话上下文。
 */
@Data
public class ChatSessionVO {
    private Long id;
    private Long userId;
    private Long sellerId;
    private Long agentId;
    private Long productId;
    private Long orderId;
    private Integer status;
    private Integer sessionType;
    private String source;
    private Integer autoReplyEnabled;
    private Integer userUnread;
    private Integer sellerUnread;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private String closeReason;
    private String userName;
    private String sellerName;
    private String productName;
    private String lastMessage;
    private String agentName;
    private Boolean isOnline;
}
