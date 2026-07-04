package com.example.minimall.service.impl;

import com.example.minimall.mapper.ChatNotificationMapper;
import com.example.minimall.model.ChatNotification;
import com.example.minimall.model.ChatMessage;
import com.example.minimall.service.ChatNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息通知服务实现
 * <p>
 * 负责：新消息通知落库 + WebSocket 实时推送。
 * 关键设计：使用 @Async 异步处理，**不阻塞主消息发送流程**。
 * </p>
 */
@Service
public class ChatNotificationServiceImpl implements ChatNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(ChatNotificationServiceImpl.class);

    /** 通知Mapper */
    @Autowired
    private ChatNotificationMapper notificationMapper;

    /** WebSocket 消息推送模板 */
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    @Async
    /**
     * 异步通知新消息（**核心方法**）
     * <p>
     * 流程：
     * <ol>
     *   <li>构造通知实体（含发送者标签、内容预览 50 字）</li>
     *   <li>落库 chat_notification 表</li>
     *   <li>通过 STOMP WebSocket 推送到 /user/{receiverId}/queue/notifications</li>
     * </ol>
     * @Async 异步执行，主消息发送链路不被通知落库阻塞。
     * </p>
     *
     * @param message      新消息实体
     * @param receiverId   接收者 ID
     * @param receiverType 接收者类型：1-用户 2-商家 3-客服
     */
    public void notifyNewMessage(ChatMessage message, Long receiverId, Integer receiverType) {
        try {
            String senderLabel = message.getSenderType() == 1 ? "买家" : message.getSenderType() == 2 ? "商家" : "管理员";
            String contentPreview = message.getContent() != null && message.getContent().length() > 50
                    ? message.getContent().substring(0, 50) + "..." : message.getContent();

            ChatNotification notification = new ChatNotification();
            notification.setUserId(receiverId);
            notification.setUserType(receiverType);
            notification.setSessionId(message.getSessionId());
            notification.setMessageId(message.getId());
            notification.setType("new_message");
            notification.setTitle("新消息 - " + senderLabel);
            notification.setContent(contentPreview);
            notification.setIsRead(0);
            notification.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notification);

            Map<String, Object> wsNotification = new HashMap<>();
            wsNotification.put("type", "notification");
            wsNotification.put("notificationId", notification.getId());
            wsNotification.put("title", notification.getTitle());
            wsNotification.put("content", notification.getContent());
            wsNotification.put("sessionId", message.getSessionId());
            wsNotification.put("createdAt", notification.getCreatedAt().toString());
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(receiverId), "/queue/notifications", wsNotification);

            logger.info("通知已创建: userId={}, type={}, sessionId={}", receiverId, "new_message", message.getSessionId());
        } catch (Exception e) {
            logger.error("创建通知失败: {}", e.getMessage());
        }
    }

    @Override
    /**
     * 分页获取用户的通知
     *
     * @param userId 用户 ID
     * @param page   页码
     * @param size   每页大小
     * @return 通知列表（按时间倒序）
     */
    public List<ChatNotification> getUserNotifications(Long userId, int page, int size) {
        return notificationMapper.selectByUserId(userId, size, (page - 1) * size);
    }

    @Override
    /**
     * 统计用户未读通知数
     *
     * @param userId 用户 ID
     * @return 未读数量
     */
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    /**
     * 标记指定通知为已读
     *
     * @param notificationId 通知 ID
     */
    public void markAsRead(Long notificationId) {
        ChatNotification notification = notificationMapper.selectById(notificationId);
        if (notification != null) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
    }

    @Override
    /** 标记所有通知为已读 */
    public void markAllAsRead(Long userId) {
        List<ChatNotification> unread = notificationMapper.selectByUserId(userId, 1000, 0);
        for (ChatNotification n : unread) {
            if (n.getIsRead() != null && n.getIsRead() == 0) {
                n.setIsRead(1);
                notificationMapper.updateById(n);
            }
        }
    }
}
