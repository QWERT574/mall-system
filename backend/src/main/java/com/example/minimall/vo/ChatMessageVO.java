package com.example.minimall.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息视图对象：包含消息内容、收发双方、消息类型与已读/送达状态等展示字段。
 */
@Data
public class ChatMessageVO {
    private Long id;
    private Long sessionId;
    private Long senderId;
    private Integer senderType;
    private Long receiverId;
    private String content;
    private String imageUrl;
    private Integer messageType;
    private String fileName;
    private Long fileSize;
    private Long relatedOrderId;
    private Long relatedProductId;
    private Integer isRead;
    private LocalDateTime readAt;
    private Integer status;
    private LocalDateTime deliveredAt;
    private Integer isAutoReply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String senderName;
}
