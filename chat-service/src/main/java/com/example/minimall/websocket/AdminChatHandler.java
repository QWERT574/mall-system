package com.example.minimall.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理员/客服 WebSocket 消息处理器。
 * <p>
 * 继承自 {@link TextWebSocketHandler}，按 URL 路径将连接分别归类为
 * 管理员会话、商家会话、普通用户会话，实现用户/商家/管理员之间的
 * 消息路由、心跳维持、上下线通知。
 * </p>
 */
@Component
public class AdminChatHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdminChatHandler.class);
    private final Map<String, WebSocketSession> adminSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sellerSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接建立后：根据 URL 路径识别角色并加入对应的 session 池。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : null;
        String sessionId = extractSessionId(session);
        logger.info("WebSocket connection established: sessionId={}, path={}", sessionId, path);

        if (path != null) {
            if (path.contains("admin")) {
                adminSessions.put(sessionId, session);
                logger.info("Admin connected: {}", sessionId);
            } else if (path.contains("seller")) {
                sellerSessions.put(sessionId, session);
                logger.info("Seller connected: {}", sessionId);
            } else {
                userSessions.put(sessionId, session);
                logger.info("User connected: {}", sessionId);
            }
        }
    }

    /**
     * 接收文本消息：处理心跳、admin_join 以及基于 target 的消息转发。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.debug("Received WebSocket message: {}", payload);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String type = (String) msg.get("type");
            String target = (String) msg.get("target");
            
            if ("heartbeat".equals(type)) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
                return;
            }

            if ("admin_join".equals(type)) {
                String targetSessionId = (String) msg.get("sessionId");
                if (targetSessionId != null) {
                    msg.put("type", "admin_joined");
                    msg.put("message", "管理员已加入对话");
                    sendToSession(targetSessionId, msg);
                }
                return;
            }

            if (target != null) {
                sendToSession(target, msg);
            }

            String sessionId = extractSessionId(session);
            logger.info("Message routed: type={}, from={}, target={}", type, sessionId, target);
        } catch (Exception e) {
            logger.error("Error processing WebSocket message: {}", payload, e);
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"消息处理失败\"}"));
        }
    }

    /**
     * 连接关闭后：从三类 session 池中清理，并向管理员广播下线通知。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = extractSessionId(session);
        logger.info("WebSocket connection closed: sessionId={}, status={}", sessionId, status);

        adminSessions.remove(sessionId);
        sellerSessions.remove(sessionId);
        userSessions.remove(sessionId);

        notifyDisconnection(sessionId);
    }

    /**
     * 传输层错误：记录日志并尝试以 SERVER_ERROR 关闭当前 session。
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket transport error for session {}", extractSessionId(session), exception);
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException e) {
            logger.error("Error closing WebSocket session", e);
        }
    }

    /**
     * 向指定 userId 的用户会话发送消息。
     */
    public void sendToUser(Long userId, Object message) {
        String sessionKey = "user_" + userId;
        broadcastToSession(userSessions, sessionKey, message);
    }

    /**
     * 向指定 sellerId 的商家会话发送消息。
     */
    public void sendToSeller(Long sellerId, Object message) {
        String sessionKey = "seller_" + sellerId;
        broadcastToSession(sellerSessions, sessionKey, message);
    }

    /**
     * 群发消息给所有在线的管理员 session。
     */
    public void sendToAdmin(Object message) {
        for (Map.Entry<String, WebSocketSession> entry : adminSessions.entrySet()) {
            try {
                if (entry.getValue().isOpen()) {
                    String json = objectMapper.writeValueAsString(message);
                    entry.getValue().sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                logger.error("Failed to send message to admin: {}", entry.getKey(), e);
            }
        }
    }

    /**
     * 获取当前在线管理员连接数。
     */
    public int getAdminCount() {
        return adminSessions.size();
    }

    /**
     * 按 sessionId 在三个 session 池中查找目标并发送消息。
     */
    private void sendToSession(String sessionId, Object message) {
        WebSocketSession targetSession = adminSessions.get(sessionId);
        if (targetSession == null) {
            targetSession = sellerSessions.get(sessionId);
        }
        if (targetSession == null) {
            targetSession = userSessions.get(sessionId);
        }

        if (targetSession != null && targetSession.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                targetSession.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                logger.error("Failed to send message to session: {}", sessionId, e);
            }
        } else {
            logger.warn("Target session not found or closed: {}", sessionId);
        }
    }

    /**
     * 在指定 session 池中按 key 发送消息，目标不存在或已关闭时跳过。
     */
    private void broadcastToSession(Map<String, WebSocketSession> sessions, String sessionKey, Object message) {
        WebSocketSession session = sessions.get(sessionKey);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                logger.error("Failed to send message to session: {}", sessionKey, e);
            }
        }
    }

    /**
     * 向所有管理员推送 user_disconnected 通知。
     */
    private void notifyDisconnection(String sessionId) {
        Map<String, Object> notification = new java.util.HashMap<>();
        notification.put("type", "user_disconnected");
        notification.put("sessionId", sessionId);
        sendToAdmin(notification);
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
