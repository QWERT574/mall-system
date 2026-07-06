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
class AdminChatHandlerTest {

    private AdminChatHandler adminChatHandler;

    @Mock
    private WebSocketSession adminSession;

    @Mock
    private WebSocketSession userSession;

    @Mock
    private WebSocketSession sellerSession;

    @Mock
    private WebSocketSession adminSession2;

    @BeforeEach
    void setUp() {
        adminChatHandler = new AdminChatHandler();
    }

    private void mockSessionAdmin(WebSocketSession session, String id) {
        when(session.getId()).thenReturn(id);
        when(session.getUri()).thenReturn(URI.create("/ws-chat/admin?sessionId=" + id + "&type=admin&adminId=5"));
        when(session.isOpen()).thenReturn(true);
    }

    private void mockSessionUser(WebSocketSession session, String userId) {
        when(session.getId()).thenReturn("user-session-" + userId);
        when(session.getUri()).thenReturn(URI.create("/ws-chat/user?sessionId=user_" + userId + "&userId=" + userId));
        when(session.isOpen()).thenReturn(true);
    }

    private void mockSessionSeller(WebSocketSession session, String sellerId) {
        when(session.getId()).thenReturn("seller-session-" + sellerId);
        when(session.getUri()).thenReturn(URI.create("/ws-chat/seller?sessionId=seller_" + sellerId + "&sellerId=" + sellerId));
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    @DisplayName("管理员连接 - 应注册session")
    void afterConnectionEstablished_admin_shouldRegisterSession() throws Exception {
        mockSessionAdmin(adminSession, "admin-1");

        adminChatHandler.afterConnectionEstablished(adminSession);

        assertEquals(1, adminChatHandler.getAdminCount());
    }

    @Test
    @DisplayName("用户连接 - 应注册session")
    void afterConnectionEstablished_user_shouldRegisterSession() throws Exception {
        mockSessionUser(userSession, "101");

        adminChatHandler.afterConnectionEstablished(userSession);

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "test");
        adminChatHandler.sendToUser(101L, msg);

        verify(userSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("商家连接 - 应注册session")
    void afterConnectionEstablished_seller_shouldRegisterSession() throws Exception {
        mockSessionSeller(sellerSession, "11");

        adminChatHandler.afterConnectionEstablished(sellerSession);

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "test");
        adminChatHandler.sendToSeller(11L, msg);

        verify(sellerSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("未知类型连接 - 不应注册")
    void afterConnectionEstablished_unknownType_shouldNotRegister() throws Exception {
        when(adminSession.getId()).thenReturn("unknown-1");
        when(adminSession.getUri()).thenReturn(URI.create("/ws-chat/unknown"));

        adminChatHandler.afterConnectionEstablished(adminSession);

        assertEquals(0, adminChatHandler.getAdminCount());
    }

    @Test
    @DisplayName("心跳消息 - 应回复pong")
    void handleTextMessage_heartbeat_shouldReplyPong() throws Exception {
        mockSessionAdmin(adminSession, "admin-1");
        adminChatHandler.afterConnectionEstablished(adminSession);

        String heartbeat = "{\"type\":\"heartbeat\"}";
        adminChatHandler.handleTextMessage(adminSession, new TextMessage(heartbeat));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(adminSession, times(1)).sendMessage(captor.capture());
        assertTrue(captor.getValue().getPayload().contains("pong"));
    }

    @Test
    @DisplayName("管理员加入房间 - 应通知目标用户")
    void handleTextMessage_admin_join_shouldNotifyTargetUser() throws Exception {
        mockSessionAdmin(adminSession, "admin-1");
        mockSessionUser(userSession, "101");
        adminChatHandler.afterConnectionEstablished(adminSession);
        adminChatHandler.afterConnectionEstablished(userSession);

        String joinMsg = "{\"type\":\"admin_join\",\"sessionId\":\"user_101\"}";
        adminChatHandler.handleTextMessage(adminSession, new TextMessage(joinMsg));

        verify(userSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendToUser - 用户在线时应发送消息")
    void sendToUser_whenOnline_shouldSendMessage() throws Exception {
        mockSessionUser(userSession, "101");
        adminChatHandler.afterConnectionEstablished(userSession);

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "admin_reply");
        msg.put("content", "管理员回复了");
        adminChatHandler.sendToUser(101L, msg);

        verify(userSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendToUser - 用户离线时不应发送")
    void sendToUser_whenOffline_shouldDoNothing() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "admin_reply");
        adminChatHandler.sendToUser(999L, msg);
    }

    @Test
    @DisplayName("sendToSeller - 商家在线时应发送消息")
    void sendToSeller_whenOnline_shouldSendMessage() throws Exception {
        mockSessionSeller(sellerSession, "11");
        adminChatHandler.afterConnectionEstablished(sellerSession);

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "admin_reply");
        msg.put("content", "已处理投诉");
        adminChatHandler.sendToSeller(11L, msg);

        verify(sellerSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("sendToAdmin - 应发送给所有管理员")
    void sendToAdmin_shouldSendToAllAdmins() throws Exception {
        mockSessionAdmin(adminSession, "admin-1");
        mockSessionAdmin(adminSession2, "admin-2");
        adminChatHandler.afterConnectionEstablished(adminSession);
        adminChatHandler.afterConnectionEstablished(adminSession2);

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "new_intervention");
        msg.put("interventionId", 1);
        adminChatHandler.sendToAdmin(msg);

        verify(adminSession, times(1)).sendMessage(any(TextMessage.class));
        verify(adminSession2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("连接断开 - 管理员断开应通知其他管理员")
    void afterConnectionClosed_adminDisconnect_shouldNotifyOtherAdmins() throws Exception {
        mockSessionAdmin(adminSession, "admin-1");
        mockSessionAdmin(adminSession2, "admin-2");
        adminChatHandler.afterConnectionEstablished(adminSession);
        adminChatHandler.afterConnectionEstablished(adminSession2);

        adminChatHandler.afterConnectionClosed(adminSession, CloseStatus.NORMAL);

        verify(adminSession2, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("getAdminCount - 应返回在线管理员数")
    void getAdminCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(0, adminChatHandler.getAdminCount());

        mockSessionAdmin(adminSession, "admin-1");
        adminChatHandler.afterConnectionEstablished(adminSession);
        assertEquals(1, adminChatHandler.getAdminCount());
    }

    @Test
    @DisplayName("处理异常 - 应关闭session")
    void handleTransportError_shouldCloseSession() throws Exception {
        mockSessionAdmin(adminSession, "admin-1");
        adminChatHandler.afterConnectionEstablished(adminSession);

        adminChatHandler.handleTransportError(adminSession, new IOException("Connection error"));

        verify(adminSession, times(1)).close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("混合连接 - 多种类型共存")
    void mixedConnections_shouldMaintainSeparateMaps() throws Exception {
        mockSessionAdmin(adminSession, "admin-1");
        mockSessionUser(userSession, "101");
        mockSessionSeller(sellerSession, "11");

        adminChatHandler.afterConnectionEstablished(adminSession);
        adminChatHandler.afterConnectionEstablished(userSession);
        adminChatHandler.afterConnectionEstablished(sellerSession);

        assertEquals(1, adminChatHandler.getAdminCount());

        Map<String, Object> adminMsg = new HashMap<>();
        adminMsg.put("type", "test");
        adminChatHandler.sendToAdmin(adminMsg);
        verify(adminSession, atLeastOnce()).sendMessage(any(TextMessage.class));

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("type", "test");
        adminChatHandler.sendToUser(101L, userMsg);
        verify(userSession, atLeastOnce()).sendMessage(any(TextMessage.class));

        Map<String, Object> sellerMsg = new HashMap<>();
        sellerMsg.put("type", "test");
        adminChatHandler.sendToSeller(11L, sellerMsg);
        verify(sellerSession, atLeastOnce()).sendMessage(any(TextMessage.class));
    }
}
