package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天通知实体，对应 chat_notification 表，向用户推送聊天相关通知
 */
@Data
@TableName("chat_notification")
public class ChatNotification {
    @TableId
    private Long id;
    private Long userId;
    private Integer userType;
    private Long sessionId;
    private Long messageId;
    private String type;
    private String title;
    private String content;
    private Integer isRead;
    private LocalDateTime createdAt;
}
