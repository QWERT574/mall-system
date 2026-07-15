package com.example.minimall.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用聊天 WebSocket 消息处理器。
 * <p>
 * 维护 sessionId 与 WebSocketSession 的映射，支持心跳保活、
 * 点对点 send_message、admin_send、加入房间等消息类型，
 * 并在连接关闭时广播 user_left 通知。
 * </p>
 */
@Component
public class ChatMessageHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接建立后登记 session，并向客户端回复 connected ACK。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = extractSessionId(session);
        sessions.put(sessionId, session);
        logger.info("Chat WebSocket connected: {}", sessionId);

        Map<String, Object> ack = new HashMap<>();
        ack.put("type", "connected");
        ack.put("sessionId", sessionId);
        sendJson(session, ack);
    }

    /**
     * 接收文本消息：分发到 heartbeat、send_message、admin_send、join 四类处理逻辑。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String type = (String) msg.get("type");

            if ("heartbeat".equals(type)) {
                HashMap<String, String> pong = new HashMap<>();
                pong.put("type", "pong");
                sendJson(session, pong);
                return;
            }

            if ("send_message".equals(type)) {
                String targetSessionId = (String) msg.get("targetSessionId");
                String content = (String) msg.get("content");
                String senderType = (String) msg.get("senderType");
                Long senderId = msg.get("senderId") != null ?
                    Long.valueOf(msg.get("senderId").toString()) : null;

                Map<String, Object> chatMsg = new HashMap<>();
                chatMsg.put("type", "new_message");
                chatMsg.put("content", content);
                chatMsg.put("senderType", senderType);
                chatMsg.put("senderId", senderId);
                chatMsg.put("timestamp", LocalDateTime.now().toString());

                if (targetSessionId != null) {
                    WebSocketSession targetSession = sessions.get(targetSessionId);
                    if (targetSession != null && targetSession.isOpen()) {
                        sendJson(targetSession, chatMsg);
                        HashMap<String, Object> ack = new HashMap<>();
                        ack.put("type", "message_sent");
                        ack.put("status", "delivered");
                        sendJson(session, ack);
                        logger.info("Message delivered: from={}, to={}, content={}", senderId, targetSessionId, content);
                    } else {
                        HashMap<String, Object> ack = new HashMap<>();
                        ack.put("type", "message_sent");
                        ack.put("status", "offline");
                        sendJson(session, ack);
                        logger.warn("Target session offline: {}", targetSessionId);
                    }
                }
                return;
            }

            if ("admin_send".equals(type)) {
                String targetSessionId = (String) msg.get("targetSessionId");
                String content = (String) msg.get("content");

                Map<String, Object> adminMsg = new HashMap<>();
                adminMsg.put("type", "admin_message");
                adminMsg.put("content", content);
                adminMsg.put("timestamp", LocalDateTime.now().toString());

                if (targetSessionId != null) {
                    WebSocketSession targetSession = sessions.get(targetSessionId);
                    if (targetSession != null && targetSession.isOpen()) {
                        sendJson(targetSession, adminMsg);
                        HashMap<String, Object> ack = new HashMap<>();
                        ack.put("type", "admin_message_sent");
                        ack.put("status", "delivered");
                        sendJson(session, ack);
                        logger.info("Admin message delivered to session: {}", targetSessionId);
                    } else {
                        HashMap<String, Object> ack = new HashMap<>();
                        ack.put("type", "admin_message_sent");
                        ack.put("status", "offline");
                        sendJson(session, ack);
                        logger.warn("Target session offline for admin message: {}", targetSessionId);
                    }
                }
                return;
            }

            if ("join".equals(type)) {
                String roomId = (String) msg.get("roomId");
                if (roomId != null) {
                    msg.put("type", "joined");
                    msg.put("message", "You have joined room: " + roomId);
                    sendJson(session, msg);
                    logger.info("User joined room: {}", roomId);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing chat message: {}", payload, e);
            HashMap<String, Object> errMsg = new HashMap<>();
            errMsg.put("type", "error");
            errMsg.put("message", "消息处理失败");
            sendJson(session, errMsg);
        }
    }

    /**
     * 连接关闭后清理 session 池并向其他在线连接广播 user_left 通知。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = extractSessionId(session);
        sessions.remove(sessionId);
        logger.info("Chat WebSocket disconnected: {}, status: {}", sessionId, status);

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "user_left");
        notification.put("sessionId", sessionId);
        broadcast(notification, sessionId);
    }

    /**
     * 传输层错误：记录日志并以 SERVER_ERROR 关闭当前 session。
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("Chat WebSocket error for session {}", extractSessionId(session), exception);
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException e) {
            logger.error("Error closing chat session", e);
        }
    }

    /**
     * 向指定 sessionId 发送消息（仅当连接存在且未关闭时）。
     */
    public void sendToSession(String sessionId, Object message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            sendJson(session, message);
        }
    }

    /**
     * 获取当前在线连接数。
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 通过 userId 查找 user_&lt;id&gt; 会话并推送新消息通知。
     */
    public void sendMessageToUser(Long userId, com.example.minimall.model.ChatMessage message) {
        HashMap<String, Object> notification = new HashMap<>();
        notification.put("type", "new_message");
        notification.put("senderId", message.getSenderId());
        notification.put("senderType", message.getSenderType());
        notification.put("content", message.getContent());
        notification.put("sessionId", message.getSessionId());
        notification.put("timestamp", message.getCreatedAt() != null ?
            message.getCreatedAt().toString() : LocalDateTime.now().toString());

        String targetKey = "user_" + userId;
        WebSocketSession targetSession = sessions.get(targetKey);
        if (targetSession != null && targetSession.isOpen()) {
            sendJson(targetSession, notification);
        }
    }

    /**
     * 按数字 sessionId 转发 ChatMessage 到目标 session。
     */
    public void broadcastToSession(Long sessionId, com.example.minimall.model.ChatMessage message) {
        sendToSession(String.valueOf(sessionId), message);
    }

    /**
     * 将对象序列化为 JSON 并发送给指定 session，失败时记录日志。
     */
    private void sendJson(WebSocketSession session, Object message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            logger.error("Failed to send JSON message to session", e);
        }
    }

    /**
     * 向所有在线 session 广播消息（可指定排除某个 sessionId）。
     */
    private void broadcast(Object message, String excludeSessionId) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            logger.error("Failed to serialize broadcast message", e);
            return;
        }

        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (excludeSessionId != null && excludeSessionId.equals(entry.getKey())) {
                continue;
            }
            try {
                if (entry.getValue().isOpen()) {
                    entry.getValue().sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                logger.error("Failed to broadcast to session: {}", entry.getKey(), e);
            }
        }
    }

    /**
     * 从 URL 查询参数中解析 sessionId，兜底使用 WebSocketSession 自带 ID。
     */
    private String extractSessionId(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null && query.contains("sessionId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("sessionId=")) {
                    return param.substring("sessionId=".length());
                }
            }
        }
        return session.getId();
    }
}
