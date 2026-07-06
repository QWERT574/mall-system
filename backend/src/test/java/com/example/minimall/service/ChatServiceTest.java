package com.example.minimall.service;

import com.example.minimall.config.TestConfig;
import com.example.minimall.mapper.ChatMessageMapper;
import com.example.minimall.mapper.ChatSessionMapper;
import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatSession;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    private static Long testSessionId;

    @Test
    @Order(1)
    void testCreateSession() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        assertNotNull(session);
        assertNotNull(session.getId());
        assertEquals(101L, session.getUserId());
        assertEquals(11L, session.getSellerId());
        assertEquals(0, session.getStatus());
        testSessionId = session.getId();
    }

    @Test
    @Order(2)
    void testGetOrCreateSessionReturnsExisting() {
        ChatSession session1 = chatService.getOrCreateSession(101L, 11L, null, null);
        ChatSession session2 = chatService.getOrCreateSession(101L, 11L, null, null);
        assertEquals(session1.getId(), session2.getId());
    }

    @Test
    @Order(3)
    void testSendTextMessage() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        ChatMessage message = chatService.sendTextMessage(
                session.getId(), 101L, 1, 11L, "测试消息", null, null);
        assertNotNull(message);
        assertNotNull(message.getId());
        assertEquals("测试消息", message.getContent());
        assertEquals(1, message.getSenderType());
        assertEquals(0, message.getIsRead());
        assertEquals(1, message.getStatus());
    }

    @Test
    @Order(4)
    void testSendImageMessage() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        ChatMessage message = chatService.sendImageMessage(
                session.getId(), 101L, 1, 11L, "http://example.com/image.jpg", null, null, null);
        assertNotNull(message);
        assertEquals(2, message.getMessageType());
        assertEquals("http://example.com/image.jpg", message.getImageUrl());
        assertEquals(1, message.getStatus());
    }

    @Test
    @Order(5)
    void testGetMessages() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        chatService.sendTextMessage(session.getId(), 101L, 1, 11L, "消息1", null, null);
        chatService.sendTextMessage(session.getId(), 11L, 2, 101L, "消息2", null, null);
        List<ChatMessage> messages = chatService.getMessages(session.getId());
        assertNotNull(messages);
        assertTrue(messages.size() >= 2);
    }

    @Test
    @Order(6)
    void testMarkMessagesAsRead() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        chatService.sendTextMessage(session.getId(), 101L, 1, 11L, "未读消息", null, null);
        chatService.markMessagesAsRead(session.getId(), 11L, 2);
        List<ChatMessage> messages = chatService.getMessages(session.getId());
        boolean allRead = messages.stream()
                .filter(m -> m.getSenderType() == 1)
                .allMatch(m -> m.getIsRead() == 1);
        assertTrue(allRead);
    }

    @Test
    @Order(7)
    void testCloseSession() {
        ChatSession session = chatService.getOrCreateSession(999L, 11L, null, null);
        chatService.closeSession(session.getId(), "test_close");
        ChatSession closed = sessionMapper.selectById(session.getId());
        assertNotNull(closed);
        assertEquals(2, closed.getStatus());
        assertEquals("test_close", closed.getCloseReason());
    }

    @Test
    @Order(8)
    void testMessageStatusTracking() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        ChatMessage message = chatService.sendTextMessage(
                session.getId(), 101L, 1, 11L, "状态跟踪测试", null, null);
        assertNotNull(message.getStatus());
        assertEquals(1, message.getStatus());
        message.setStatus(2);
        message.setDeliveredAt(java.time.LocalDateTime.now());
        chatService.updateMessage(message);
        ChatMessage updated = messageMapper.selectById(message.getId());
        assertEquals(2, updated.getStatus());
        assertNotNull(updated.getDeliveredAt());
    }
}
