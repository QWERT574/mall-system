package com.example.minimall.integration;

import com.example.minimall.config.TestConfig;
import com.example.minimall.common.Result;
import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatSession;
import com.example.minimall.model.User;
import com.example.minimall.service.ChatService;
import com.example.minimall.service.RedisChatService;
import com.example.minimall.utils.JwtUtil;
import com.example.minimall.mapper.ChatMessageMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatMessageDeliveryE2ETest {

    /**
     * 用于为每个测试方法分配独立 productId, 从而让 getOrCreateSession 返回独立 session,
     * 避免跨测试/跨运行的消息状态污染。
     */
    private static final AtomicLong PRODUCT_ID_GEN = new AtomicLong(
            System.currentTimeMillis() % 100_000L);


    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMessageMapper messageMapper;

    private String userToken;
    private String sellerToken;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(101L);
        user.setUsername("testuser");
        userToken = jwtUtil.generateToken(user);

        User seller = new User();
        seller.setId(11L);
        seller.setUsername("testseller");
        sellerToken = jwtUtil.generateToken(seller);
    }

    @Test
    @Order(1)
    void testFullMessageDeliveryFlow() {
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.set("Authorization", "Bearer " + userToken);
        userHeaders.set("User-Type", "1");

        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        assertNotNull(session);

        String sendUrl = "http://localhost:" + port + "/api/chat/message?sessionId=" + session.getId() + "&content=E2E测试消息";
        HttpEntity<Void> sendEntity = new HttpEntity<>(userHeaders);
        ResponseEntity<Result> sendResponse = restTemplate.exchange(sendUrl, HttpMethod.POST, sendEntity, Result.class);
        assertEquals(HttpStatus.OK, sendResponse.getStatusCode());
        assertEquals(0, sendResponse.getBody().getCode());

        List<ChatMessage> messages = chatService.getMessages(session.getId());
        assertFalse(messages.isEmpty());
        ChatMessage sentMessage = messages.stream()
                .filter(m -> "E2E测试消息".equals(m.getContent()))
                .findFirst()
                .orElse(null);
        assertNotNull(sentMessage);
        assertEquals(1, sentMessage.getSenderType());
        assertNotNull(sentMessage.getStatus());
        assertTrue(sentMessage.getStatus() >= 1);
    }

    @Test
    @Order(2)
    void testMessageStatusTransition() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        ChatMessage message = chatService.sendTextMessage(
                session.getId(), 101L, 1, 11L, "状态转换测试", null, null);
        assertEquals(1, message.getStatus());

        message.setStatus(2);
        message.setDeliveredAt(LocalDateTime.now());
        messageMapper.updateById(message);
        ChatMessage delivered = messageMapper.selectById(message.getId());
        assertEquals(2, delivered.getStatus());
        assertNotNull(delivered.getDeliveredAt());

        delivered.setStatus(3);
        delivered.setIsRead(1);
        delivered.setReadAt(LocalDateTime.now());
        messageMapper.updateById(delivered);
        ChatMessage read = messageMapper.selectById(message.getId());
        assertEquals(3, read.getStatus());
        assertEquals(1, read.getIsRead());
        assertNotNull(read.getReadAt());
    }

    @Test
    @Order(3)
    void testSellerCanViewBuyerMessages() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        chatService.sendTextMessage(session.getId(), 101L, 1, 11L, "买家消息1", null, null);
        chatService.sendTextMessage(session.getId(), 101L, 1, 11L, "买家消息2", null, null);

        HttpHeaders sellerHeaders = new HttpHeaders();
        sellerHeaders.set("Authorization", "Bearer " + sellerToken);
        sellerHeaders.set("User-Type", "2");

        String url = "http://localhost:" + port + "/api/chat/messages/" + session.getId();
        HttpEntity<Void> entity = new HttpEntity<>(sellerHeaders);
        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.GET, entity, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());
    }

    @Test
    @Order(4)
    void testSellerCanReply() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        chatService.sendTextMessage(session.getId(), 101L, 1, 11L, "买家提问", null, null);

        HttpHeaders sellerHeaders = new HttpHeaders();
        sellerHeaders.set("Authorization", "Bearer " + sellerToken);
        sellerHeaders.set("User-Type", "2");

        String url = "http://localhost:" + port + "/api/chat/message?sessionId=" + session.getId() + "&content=商家回复";
        HttpEntity<Void> entity = new HttpEntity<>(sellerHeaders);
        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.POST, entity, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCode());

        List<ChatMessage> messages = chatService.getMessages(session.getId());
        ChatMessage reply = messages.stream()
                .filter(m -> "商家回复".equals(m.getContent()))
                .findFirst()
                .orElse(null);
        assertNotNull(reply);
        assertEquals(2, reply.getSenderType());
    }

    @Test
    @Order(5)
    void testMessagePersistence() {
        Long uniqueProductId = PRODUCT_ID_GEN.incrementAndGet();
        ChatSession session = chatService.getOrCreateSession(101L, 11L, uniqueProductId, null);
        for (int i = 0; i < 5; i++) {
            chatService.sendTextMessage(session.getId(), 101L, 1, 11L, "持久化测试" + i, null, null);
        }
        List<ChatMessage> messages = chatService.getMessages(session.getId());
        long persistedCount = messages.stream()
                .filter(m -> m.getContent() != null && m.getContent().startsWith("持久化测试"))
                .count();
        assertEquals(5, persistedCount);
    }

    @Test
    @Order(6)
    void testConcurrentMessageSending() throws InterruptedException {
        Long uniqueProductId = PRODUCT_ID_GEN.incrementAndGet();
        ChatSession session = chatService.getOrCreateSession(101L, 11L, uniqueProductId, null);
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                chatService.sendTextMessage(session.getId(), 101L, 1, 11L, "并发消息" + idx, null, null);
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join(5000);
        }
        List<ChatMessage> messages = chatService.getMessages(session.getId());
        long concurrentCount = messages.stream()
                .filter(m -> m.getContent() != null && m.getContent().startsWith("并发消息"))
                .count();
        assertEquals(10, concurrentCount);
    }
}
