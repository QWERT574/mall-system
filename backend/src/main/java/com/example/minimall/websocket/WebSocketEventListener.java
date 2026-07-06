package com.example.minimall.websocket;

import com.example.minimall.config.WebSocketAuthInterceptor;
import com.example.minimall.service.RedisChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 连接/断连事件监听器。
 * <p>
 * 监听 {@link SessionConnectedEvent} 与 {@link SessionDisconnectEvent}，
 * 将用户/客服/商家的在线状态同步到 Redis，
 * 供业务层做在线状态查询与消息推送路由。
 * </p>
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final RedisChatService redisChatService;

    /**
     * 构造方法，注入 Redis 聊天服务用于维护在线状态。
     */
    public WebSocketEventListener(RedisChatService redisChatService) {
        this.redisChatService = redisChatService;
    }

    /**
     * STOMP 连接成功事件：根据 User-Type 头区分用户/客服/商家并写入 Redis 在线集合。
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() instanceof WebSocketAuthInterceptor.StompPrincipal) {
            WebSocketAuthInterceptor.StompPrincipal principal =
                (WebSocketAuthInterceptor.StompPrincipal) accessor.getUser();
            if (principal == null) return;
            Long userId = principal.getUserId();
            String userType = accessor.getFirstNativeHeader("User-Type");

            if ("agent".equals(userType) || "seller".equals(userType) || "admin".equals(userType)) {
                redisChatService.agentOnline(userId);
                logger.info("Agent/Seller connected via WebSocket: userId={}, type={}", userId, userType);
            } else {
                redisChatService.userOnline(userId);
                logger.info("User connected via WebSocket: userId={}", userId);
            }
        }
    }

    /**
     * STOMP 断开连接事件：清理 Redis 中的用户/客服在线标记。
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() instanceof WebSocketAuthInterceptor.StompPrincipal) {
            WebSocketAuthInterceptor.StompPrincipal principal =
                (WebSocketAuthInterceptor.StompPrincipal) accessor.getUser();
            if (principal == null) return;
            Long userId = principal.getUserId();

            redisChatService.userOffline(userId);
            redisChatService.agentOffline(userId);
            logger.info("User disconnected via WebSocket: userId={}", userId);
        }
    }
}
