package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话实体，对应 chat_session 表，存储用户与商家或客服的会话上下文
 */
@Data
@TableName("chat_session")
public class ChatSession {
    @TableId
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

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String sellerName;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String lastMessage;

    @TableField(exist = false)
    private String agentName;

    @TableField(exist = false)
    private Boolean isOnline;
}
