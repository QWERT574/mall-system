package com.example.minimall.websocket;

import com.example.minimall.config.WebSocketAuthInterceptor;
import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatSession;
import com.example.minimall.service.ChatService;
import com.example.minimall.service.RedisChatService;
import com.example.minimall.service.SensitiveWordFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * STOMP 聊天消息控制器。
 * <p>
 * 通过 {@code @MessageMapping} 监听 /tcp/chat/* 目的地：处理文本/图片/文件消息、
 * 已读回执、正在输入状态；将消息写入数据库并通过 SimpMessagingTemplate
 * 推送到对应 session 主题与用户队列；附带敏感词过滤与自动回复。
 * </p>
 */
@Controller
public class ChatStompController {

    private static final Logger logger = LoggerFactory.getLogger(ChatStompController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final RedisChatService redisChatService;
    private final SensitiveWordFilter sensitiveWordFilter;

    /**
     * 构造方法，注入消息模板、聊天服务、Redis 聊天服务与敏感词过滤器。
     */
    public ChatStompController(SimpMessagingTemplate messagingTemplate, ChatService chatService,
                               RedisChatService redisChatService,
                               SensitiveWordFilter sensitiveWordFilter) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.redisChatService = redisChatService;
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

    /**
     * 处理 /tcp/chat/send 消息：校验会话/敏感词后落库并广播到对应 session 主题，
     * 同时向发送方 ACK、向接收方推送通知；必要时触发自动回复。
     */
    @MessageMapping("/chat/send")
    public void handleChatMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long senderId = getUserId(principal);
        Long sessionId = toLong(payload.get("sessionId"));
        String content = (String) payload.get("content");
        Integer messageType = payload.get("messageType") != null ?
            Integer.valueOf(payload.get("messageType").toString()) : 1;
        String imageUrl = (String) payload.get("imageUrl");
        String fileName = (String) payload.get("fileName");
        Long fileSize = payload.get("fileSize") != null ?
            Long.valueOf(payload.get("fileSize").toString()) : null;
        String clientMsgId = (String) payload.get("clientMsgId");

        if (sessionId == null || content == null || content.trim().isEmpty()) {
            sendError(senderId, "消息内容不能为空");
            return;
        }

        if (sensitiveWordFilter.containsSensitiveWord(content)) {
            Map<String, Object> rejected = new HashMap<>();
            rejected.put("type", "message_rejected");
            rejected.put("clientMsgId", clientMsgId);
            rejected.put("reason", "消息包含敏感内容，无法发送");
            messagingTemplate.convertAndSendToUser(String.valueOf(senderId), "/queue/notifications", rejected);
            return;
        }

        ChatSession session = chatService.getSessionDetail(sessionId);
        if (session == null) {
            sendError(senderId, "会话不存在");
            return;
        }

        if (!senderId.equals(session.getUserId()) && !senderId.equals(session.getAgentId())
            && !senderId.equals(session.getSellerId())) {
            sendError(senderId, "无权在此会话发送消息");
            return;
        }

        Integer senderType = determineSenderType(senderId, session);

        ChatMessage message;
        if (messageType == 2) {
            message = chatService.sendImageMessage(sessionId, senderId, senderType, null,
                imageUrl, content, null, null);
        } else if (messageType == 3) {
            message = chatService.sendFileMessage(sessionId, senderId, senderType, null,
                content, fileName, fileSize, imageUrl);
        } else {
            message = chatService.sendTextMessage(sessionId, senderId, senderType, null,
                content, null, null);
        }

        redisChatService.updateSessionActiveTime(sessionId);
        redisChatService.cacheLastMessage(sessionId, truncateContent(content));

        Map<String, Object> msgData = buildMessageData(message, clientMsgId);
        messagingTemplate.convertAndSend("/topic/chat/session/" + sessionId, msgData);

        Map<String, Object> ack = new HashMap<>();
        ack.put("type", "message_sent");
        ack.put("clientMsgId", clientMsgId);
        ack.put("serverMsgId", message.getId());
        ack.put("timestamp", message.getCreatedAt().toString());
        messagingTemplate.convertAndSendToUser(String.valueOf(senderId), "/queue/ack", ack);

        pushNotificationToOtherParty(session, senderId, senderType, message);

        boolean autoReplyEnabled = session.getAutoReplyEnabled() != null && session.getAutoReplyEnabled() == 1;
        if (senderType == 1 && autoReplyEnabled) {
            handleAutoReply(session, message);
        }
    }

    /**
     * 发送一条自动回复文本消息并广播到对应 session 主题。
     */
    private void handleAutoReply(ChatSession session, ChatMessage userMessage) {
        try {
            ChatMessage autoReply = chatService.sendTextMessage(
                session.getId(), 0L, 3, session.getUserId(),
                "您好！您的消息已收到，客服人员将尽快回复您。", null, null);
            autoReply.setIsAutoReply(1);
            chatService.updateMessage(autoReply);

            Map<String, Object> autoReplyData = buildMessageData(autoReply, null);
            autoReplyData.put("isAutoReply", true);
            messagingTemplate.convertAndSend("/topic/chat/session/" + session.getId(), autoReplyData);
        } catch (Exception e) {
            logger.warn("Auto reply failed for session {}", session.getId(), e);
        }
    }

    /**
     * 处理 /tcp/chat/read：将会话消息标记为已读并广播已读事件给对方。
     */
    @MessageMapping("/chat/read")
    public void handleReadReceipt(@Payload Map<String, Object> payload, Principal principal) {
        Long userId = getUserId(principal);
        Long sessionId = toLong(payload.get("sessionId"));

        if (sessionId == null) return;

        chatService.markMessagesAsRead(sessionId, userId, 1);
        redisChatService.clearUnreadCount(sessionId, userId);

        ChatSession session = chatService.getSessionDetail(sessionId);

        Map<String, Object> readData = new HashMap<>();
        readData.put("type", "message_read");
        readData.put("sessionId", sessionId);
        readData.put("readBy", userId);
        readData.put("readAt", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/chat/session/" + sessionId, readData);

        if (session != null) {
            Long notifyTarget = userId.equals(session.getUserId()) ?
                (session.getAgentId() != null ? session.getAgentId() : session.getSellerId())
                : session.getUserId();
            messagingTemplate.convertAndSendToUser(String.valueOf(notifyTarget), "/queue/notifications", readData);
        }
    }

    /**
     * 处理 /tcp/chat/typing：广播正在输入状态变更事件到对应 session 主题。
     */
    @MessageMapping("/chat/typing")
    public void handleTyping(@Payload Map<String, Object> payload, Principal principal) {
        Long userId = getUserId(principal);
        Long sessionId = toLong(payload.get("sessionId"));
        Boolean isTyping = Boolean.TRUE.equals(payload.get("isTyping"));

        if (sessionId == null) return;

        Map<String, Object> typingData = new HashMap<>();
        typingData.put("type", "typing");
        typingData.put("sessionId", sessionId);
        typingData.put("userId", userId);
        typingData.put("isTyping", isTyping);

        messagingTemplate.convertAndSend("/topic/chat/session/" + sessionId, typingData);
    }

    /**
     * 根据发送者与会话关系判断其身份（买家 1/客服 3/卖家 2）。
     */
    private Integer determineSenderType(Long senderId, ChatSession session) {
        if (senderId.equals(session.getUserId())) return 1;
        if (session.getAgentId() != null && senderId.equals(session.getAgentId())) return 3;
        if (senderId.equals(session.getSellerId())) return 2;
        return 1;
    }

    /**
     * 向会话的另一方推送新消息通知并增加未读计数。
     */
    private void pushNotificationToOtherParty(ChatSession session, Long senderId,
                                               Integer senderType, ChatMessage message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "new_message");
        notification.put("sessionId", session.getId());
        notification.put("content", truncateContent(message.getContent()));
        notification.put("senderType", senderType);
        notification.put("createdAt", message.getCreatedAt().toString());

        if (senderType == 1) {
            Long targetId = session.getAgentId() != null ? session.getAgentId() : session.getSellerId();
            redisChatService.incrementUnreadCount(session.getId(), targetId);
            messagingTemplate.convertAndSendToUser(String.valueOf(targetId), "/queue/notifications", notification);
        } else {
            redisChatService.incrementUnreadCount(session.getId(), session.getUserId());
            messagingTemplate.convertAndSendToUser(String.valueOf(session.getUserId()),
                "/queue/notifications", notification);
        }
    }

    /**
     * 将 ChatMessage 实体组装为 STOMP 推送使用的消息数据 Map。
     */
    private Map<String, Object> buildMessageData(ChatMessage message, String clientMsgId) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "new_message");
        data.put("id", message.getId());
        data.put("sessionId", message.getSessionId());
        data.put("senderId", message.getSenderId());
        data.put("senderType", message.getSenderType());
        data.put("content", message.getContent());
        data.put("messageType", message.getMessageType());
        data.put("imageUrl", message.getImageUrl());
        data.put("fileName", message.getFileName());
        data.put("fileSize", message.getFileSize());
        data.put("isAutoReply", message.getIsAutoReply() != null && message.getIsAutoReply() == 1);
        data.put("isRead", message.getIsRead());
        data.put("createdAt", message.getCreatedAt().toString());
        data.put("clientMsgId", clientMsgId);
        return data;
    }

    /**
     * 向指定用户推送错误消息（通过 /queue/notifications 私推）。
     */
    private void sendError(Long userId, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", "error");
        error.put("message", message);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", error);
    }

    /**
     * 截断消息内容到 100 字符，用于通知预览。
     */
    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }

    /**
     * 从 STOMP Principal 中提取 userId（兼容 StompPrincipal 与普通字符串 Principal）。
     */
    private Long getUserId(Principal principal) {
        if (principal instanceof WebSocketAuthInterceptor.StompPrincipal) {
            return ((WebSocketAuthInterceptor.StompPrincipal) principal).getUserId();
        }
        if (principal != null && principal.getName() != null) {
            try {
                return Long.valueOf(principal.getName());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 通用对象转 Long 工具：支持 Number 与字符串。
     */
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
