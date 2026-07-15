package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体，对应 chat_message 表，存储用户、商家、客服间的聊天消息
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId
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

    @TableField(exist = false)
    private String senderName;
}
