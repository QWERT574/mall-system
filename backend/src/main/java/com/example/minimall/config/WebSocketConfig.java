package com.example.minimall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket 消息代理配置。
 * <p>
 * 启用 STOMP 消息代理，注册 /ws-chat 端点（兼容 SockJS），
 * 将 /topic、/user 作为广播与点对点目的地，前缀 /tcp 作为应用入口，
 * 并注册 WebSocketAuthInterceptor 鉴权。
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * 构造方法，注入 WebSocket 鉴权拦截器。
     */
    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    /**
     * 配置消息代理：开启简单内存代理，配置应用目的地与点对点用户前缀。
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/user");
        registry.setApplicationDestinationPrefixes("/tcp");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 注册 STOMP 端点 /ws-chat，开启 SockJS 兼容并放行跨域。
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 配置客户端入站通道：注册 WebSocket 鉴权拦截器。
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }

    /**
     * 配置 WebSocket 传输参数：限制单消息大小、发送缓冲与发送超时。
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(65536);
        registration.setSendBufferSizeLimit(524288);
        registration.setSendTimeLimit(20000);
    }
}
