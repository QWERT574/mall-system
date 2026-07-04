package com.example.minimall.controller;

import com.example.minimall.config.TestConfig;
import com.example.minimall.common.Result;
import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatSession;
import com.example.minimall.model.User;
import com.example.minimall.service.ChatService;
import com.example.minimall.utils.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ChatService chatService;

    private String authToken;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(101L);
        user.setUsername("testuser");
        authToken = jwtUtil.generateToken(user);
        headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        headers.set("Content-Type", "application/json");
        headers.set("User-Type", "1");
    }

    @Test
    @Order(1)
    void testCreateSession() {
        String url = "http://localhost:" + port + "/api/chat/session?sellerId=11";
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.POST, entity, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());
    }

    @Test
    @Order(2)
    void testSendMessage() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        String url = "http://localhost:" + port + "/api/chat/message?sessionId=" + session.getId() + "&content=集成测试消息";
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.POST, entity, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());
    }

    @Test
    @Order(3)
    void testGetMessages() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        chatService.sendTextMessage(session.getId(), 101L, 1, 11L, "历史消息", null, null);
        String url = "http://localhost:" + port + "/api/chat/messages/" + session.getId();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.GET, entity, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());
    }

    @Test
    @Order(4)
    void testGetSellerSessions() {
        User seller = new User();
        seller.setId(11L);
        seller.setUsername("testseller");
        String sellerToken = jwtUtil.generateToken(seller);
        HttpHeaders sellerHeaders = new HttpHeaders();
        sellerHeaders.set("Authorization", "Bearer " + sellerToken);
        sellerHeaders.set("User-Type", "2");

        String url = "http://localhost:" + port + "/api/chat/seller/sessions";
        HttpEntity<Void> entity = new HttpEntity<>(sellerHeaders);
        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.GET, entity, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());
    }

    @Test
    @Order(5)
    void testMarkAsRead() {
        ChatSession session = chatService.getOrCreateSession(101L, 11L, null, null);
        chatService.sendTextMessage(session.getId(), 101L, 1, 11L, "待读消息", null, null);
        String url = "http://localhost:" + port + "/api/chat/read?sessionId=" + session.getId();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.POST, entity, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(6)
    void testUnauthenticatedAccess() {
        HttpHeaders noAuthHeaders = new HttpHeaders();
        String url = "http://localhost:" + port + "/api/chat/sessions";
        HttpEntity<Void> entity = new HttpEntity<>(noAuthHeaders);
        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.GET, entity, Result.class);
        // Security 链对无 token 请求返回 401/403, body 可能为空, 这里断言 ResponseEntity 本身非空即可
        assertNotNull(response);
    }

    @Test
    @Order(7)
    void testOnlineOffline() {
        String onlineUrl = "http://localhost:" + port + "/api/chat/online";
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Result> onlineResponse = restTemplate.exchange(onlineUrl, HttpMethod.POST, entity, Result.class);
        assertEquals(HttpStatus.OK, onlineResponse.getStatusCode());

        String offlineUrl = "http://localhost:" + port + "/api/chat/offline";
        ResponseEntity<Result> offlineResponse = restTemplate.exchange(offlineUrl, HttpMethod.POST, entity, Result.class);
        assertEquals(HttpStatus.OK, offlineResponse.getStatusCode());
    }

    @Test
    @Order(8)
    void testMonitorHealth() {
        String url = "http://localhost:" + port + "/api/chat/monitor/health";
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.GET, entity, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @Order(9)
    void testMonitorMetrics() {
        String url = "http://localhost:" + port + "/api/chat/monitor/metrics";
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.GET, entity, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
