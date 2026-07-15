package com.example.minimall.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天系统通知视图对象：用于在消息中心/通知列表中展示新消息提醒。
 */
@Data
public class ChatNotificationVO {
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
