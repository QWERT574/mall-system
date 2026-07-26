package com.example.minimall.service;

import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatNotification;

import java.util.List;

/** 聊天消息通知服务接口 */
public interface ChatNotificationService {

    /** 推送新消息通知 */
    void notifyNewMessage(ChatMessage message, Long receiverId, Integer receiverType);

    /** 分页获取用户通知 */
    List<ChatNotification> getUserNotifications(Long userId, int page, int size);

    /** 获取未读通知数量 */
    int getUnreadCount(Long userId);

    /** 标记通知为已读 */
    void markAsRead(Long notificationId);

    /** 标记所有通知为已读 */
    void markAllAsRead(Long userId);
}
