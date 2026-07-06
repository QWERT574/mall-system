package com.example.minimall.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatMessageHandlerTest {

    private ChatMessageHandler chatMessageHandler;

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @BeforeEach
    void setUp() {
        chatMessageHandler = new ChatMessageHandler();
    }

    private void mockSession(WebSocketSession session, String id, String uri) {
        when(session.getId()).thenReturn(id);
        when(session.getUri()).thenReturn(URI.create(uri));
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    @DisplayName("建立连接 - 应发送连接确认消息")
    void afterConnectionEstablished_shouldSendConnectedAck() throws Exception {
        mockSession(session1, "session-1", "/ws-chat/user?userId=101");

        chatMessageHandler.afterConnectionEstablished(session1);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1, times(1)).sendMessage(captor.capture());
        String payload = captor.getValue().getPayload();
        assertTrue(payload.contains("connected"));
        assertTrue(payload.contains("session-1"));
    }

    @Test
    @DisplayName("心跳消息 - 应回复pong")
    void handleTextMessage_heartbeat_shouldReplyPong() throws Exception {
        mockSession(session1, "session-1", "/ws-chat/user?userId=101");

        chatMessageHandler.afterConnectionEstablished(session1);

        String heartbeat = "{\"type\":\"heartbeat\"}";
        chatMessageHandler.handleTextMessage(session1, new TextMessage(heartbeat));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1, times(2)).sendMessage(captor.capture());
        String payload = captor.getValue().getPayload();
        assertTrue(payload.contains("pong"));
    }

    @Test
    @DisplayName("加入会话 - 应注册sessionId映射")
    void handleTextMessage_join_shouldRegisterSessionMapping() throws Exception {
        mockSession(session1, "session-1", "/ws-chat/user?userId=101");

        chatMessageHandler.afterConnectionEstablished(session1);

        String joinMessage = "{\"type\":\"join\",\"roomId\":\"100\"}";
        chatMessageHandler.handleTextMessage(session1, new TextMessage(joinMessage));

        assertEquals(1, chatMessageHandler.getActiveSessionCount());
    }

    @Test
    @DisplayName("发送消息 - 应路由到接收方")
    void handleTextMessage_sendMessage_shouldRouteToRecipient() throws Exception {
        mockSession(session1, "user-session-1", "/ws-chat/user?userId=101");
        mockSession(session2, "seller-session-1", "/ws-chat/seller?sellerId=11");

        chatMessageHandler.afterConnectionEstablished(session1);
        chatMessageHandler.afterConnectionEstablished(session2);

        String sendMsg = "{\"type\":\"send_message\",\"targetSessionId\":\"seller-session-1\",\"content\":\"你好\",\"senderId\":101}";
        chatMessageHandler.handleTextMessage(session1, new TextMessage(sendMsg));

        verify(session2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("管理员发送消息 - 应发送给目标用户")
    void handleTextMessage_adminSend_shouldSendToTarget() throws Exception {
        mockSession(session1, "user-1", "/ws-chat/user?userId=101");
        mockSession(session2, "user-2", "/ws-chat/user?userId=102");

        chatMessageHandler.afterConnectionEstablished(session1);
        chatMessageHandler.afterConnectionEstablished(session2);

        String adminMsg = "{\"type\":\"admin_send\",\"targetSessionId\":\"user-2\",\"content\":\"管理员消息\"}";
        chatMessageHandler.handleTextMessage(session1, new TextMessage(adminMsg));

        verify(session2, times(2)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendToSession - 应发送消息给指定的session")
    void sendToSession_shouldSendToCorrectSession() throws Exception {
        mockSession(session1, "user-1", "/ws-chat/user?userId=101");

        chatMessageHandler.afterConnectionEstablished(session1);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "admin_reply");
        message.put("content", "管理员已回复");
        chatMessageHandler.sendToSession("user-1", message);

        verify(session1, times(2)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendToSession - 不存在的会话应静默处理")
    void sendToSession_whenNotExists_shouldDoNothing() {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "test");
        chatMessageHandler.sendToSession("999", message);
    }

    @Test
    @DisplayName("sendMessageToUser - 应只发送给目标用户，不发给其他用户")
    void sendMessageToUser_shouldOnlySendToTargetUser() throws Exception {
        mockSession(session1, "user_101", "/ws-chat/user?userId=101");
        mockSession(session2, "user_102", "/ws-chat/user?userId=102");

        chatMessageHandler.afterConnectionEstablished(session1);
        chatMessageHandler.afterConnectionEstablished(session2);

        com.example.minimall.model.ChatMessage msg = new com.example.minimall.model.ChatMessage();
        msg.setContent("商家回复");
        msg.setSenderId(11L);
        msg.setSenderType(2);
        msg.setSessionId(100L);
        msg.setReceiverId(101L);
        msg.setCreatedAt(java.time.LocalDateTime.now());

        chatMessageHandler.sendMessageToUser(101L, msg);

        verify(session1, times(2)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendMessageToUser - 不存在的用户应静默处理")
    void sendMessageToUser_whenNotExists_shouldDoNothing() throws Exception {
        mockSession(session1, "user_101", "/ws-chat/user?userId=101");
        chatMessageHandler.afterConnectionEstablished(session1);

        com.example.minimall.model.ChatMessage msg = new com.example.minimall.model.ChatMessage();
        msg.setContent("test");
        msg.setSenderId(11L);
        msg.setSenderType(2);
        msg.setSessionId(100L);
        msg.setReceiverId(999L);

        chatMessageHandler.sendMessageToUser(999L, msg);

        verify(session1, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("连接断开 - 应广播用户离开消息")
    void afterConnectionClosed_shouldBroadcastUserLeft() throws Exception {
        mockSession(session1, "user-1", "/ws-chat/user?userId=101");
        mockSession(session2, "user-2", "/ws-chat/user?userId=102");

        chatMessageHandler.afterConnectionEstablished(session1);
        chatMessageHandler.afterConnectionEstablished(session2);

        chatMessageHandler.afterConnectionClosed(session1, null);

        verify(session2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("获取活跃会话数 - 返回正确的会话数")
    void getActiveSessionCount_shouldReturnCorrectCount() throws Exception {
        mockSession(session1, "user-1", "/ws-chat/user?userId=101");

        assertEquals(0, chatMessageHandler.getActiveSessionCount());

        chatMessageHandler.afterConnectionEstablished(session1);
        assertEquals(1, chatMessageHandler.getActiveSessionCount());
    }

    @Test
    @DisplayName("处理异常 - 应关闭session")
    void handleTransportError_shouldRemoveSession() throws Exception {
        mockSession(session1, "user-1", "/ws-chat/user?userId=101");

        chatMessageHandler.afterConnectionEstablished(session1);
        assertEquals(1, chatMessageHandler.getActiveSessionCount());

        chatMessageHandler.handleTransportError(session1, new IOException("Connection lost"));

        verify(session1, times(1)).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("无效JSON消息 - 应发送错误消息")
    void handleTextMessage_invalidJson_shouldSendError() throws Exception {
        mockSession(session1, "session-1", "/ws-chat/user?userId=101");

        chatMessageHandler.afterConnectionEstablished(session1);

        String invalidJson = "this is not json";
        chatMessageHandler.handleTextMessage(session1, new TextMessage(invalidJson));

        verify(session1, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("未知消息类型 - 应静默处理")
    void handleTextMessage_unknownType_shouldIgnore() throws Exception {
        mockSession(session1, "session-1", "/ws-chat/user?userId=101");

        chatMessageHandler.afterConnectionEstablished(session1);

        String unknownMsg = "{\"type\":\"unknown_type\",\"data\":\"test\"}";
        chatMessageHandler.handleTextMessage(session1, new TextMessage(unknownMsg));

        verify(session1, times(1)).sendMessage(any(TextMessage.class));
    }
}
