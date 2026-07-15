package com.example.minimall.config;

import com.example.minimall.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * WebSocket (STOMP) 鉴权拦截器。
 * <p>
 * 实现 Spring {@link ChannelInterceptor}，拦截客户端入站消息。
 * 在 STOMP CONNECT 阶段校验请求头中的 JWT，并构建 {@link Principal} 写入会话，
 * 供业务层通过 {@code Principal#getName()} 取得登录用户。
 * </p>
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtUtil jwtUtil;

    /**
     * 构造方法，注入 JWT 工具类。
     */
    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 拦截 STOMP 消息：仅在 CONNECT 阶段进行 JWT 校验，校验失败抛出鉴权异常。
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (token != null && jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);

                Principal principal = new StompPrincipal(userId, username);
                accessor.setUser(principal);

                logger.debug("WebSocket authenticated: userId={}, username={}", userId, username);
            } else {
                logger.warn("WebSocket authentication failed: invalid or missing token");
                throw new org.springframework.security.access.AccessDeniedException("Invalid WebSocket token");
            }
        }

        return message;
    }

    /**
     * STOMP 连接的 {@link Principal} 实现：携带用户 ID 与用户名。
     */
    public static class StompPrincipal implements Principal {
        private final Long userId;
        private final String name;

        /**
         * 构造 Principal。
         *
         * @param userId 用户 ID
         * @param name   用户名
         */
        public StompPrincipal(Long userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        /**
         * 获取用户 ID。
         */
        public Long getUserId() {
            return userId;
        }

        /**
         * 获取 Principal 名（用户名）。
         */
        @Override
        public String getName() {
            return name;
        }
    }
}
