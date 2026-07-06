package com.example.minimall.controller;

import com.example.minimall.mapper.ChatMessageMapper;
import com.example.minimall.mapper.ChatSessionMapper;
import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatSession;
import com.example.minimall.service.ChatService;
import com.example.minimall.service.RedisChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminChatController 单元测试。
 * 严格匹配实际 Controller 实现 (AdminChatController)：
 *  - GET /api/admin/chat/sessions
 *  - GET /api/admin/chat/sessions/{id}
 *  - GET /api/admin/chat/messages/{sessionId}
 *  - PUT /api/admin/chat/session/{id}/close
 *  - GET /api/admin/chat/active-count
 *
 * 注: sendMessage 端点不在当前 Controller 中, 已在生产代码 ChatController 中提供, 不在本测试范围。
 */
@ExtendWith(MockitoExtension.class)
class AdminChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatService chatService;

    @Mock
    private ChatSessionMapper chatSessionMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private RedisChatService redisChatService;

    @InjectMocks
    private AdminChatController adminChatController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminChatController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("获取会话列表 - 返回 List<ChatSession>")
    void getSessions_shouldReturnList() throws Exception {
        ChatSession session = new ChatSession();
        session.setId(1L);
        session.setUserId(101L);
        session.setStatus(0);
        when(chatSessionMapper.selectList(any())).thenReturn(Arrays.asList(session));
        when(redisChatService.getLastMessage(anyLong())).thenReturn("最后消息");

        mockMvc.perform(get("/api/admin/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)));
    }

    @Test
    @DisplayName("获取会话详情 - 存在时返回数据")
    void getSessionDetail_whenExists_shouldReturnData() throws Exception {
        ChatSession session = new ChatSession();
        session.setId(1L);
        session.setUserId(101L);
        session.setStatus(0);
        when(chatSessionMapper.selectSessionDetail(1L)).thenReturn(session);

        mockMvc.perform(get("/api/admin/chat/sessions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.id", is(1)));
    }

    @Test
    @DisplayName("获取会话详情 - 不存在时返回错误")
    void getSessionDetail_whenNotExists_shouldReturnError() throws Exception {
        when(chatSessionMapper.selectSessionDetail(999L)).thenReturn(null);

        mockMvc.perform(get("/api/admin/chat/sessions/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)));
    }

    @Test
    @DisplayName("获取会话消息 - 存在时应返回消息列表")
    void getMessages_shouldReturnList() throws Exception {
        ChatMessage msg = new ChatMessage();
        msg.setId(1L);
        msg.setSessionId(1L);
        msg.setContent("测试消息");
        when(chatService.getMessages(1L)).thenReturn(Arrays.asList(msg));

        mockMvc.perform(get("/api/admin/chat/messages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("获取会话消息 - 无消息时返回空列表")
    void getMessages_whenNoMessages_shouldReturnEmpty() throws Exception {
        when(chatService.getMessages(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/chat/messages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("关闭会话 - PUT /session/{id}/close 应调用 chatService")
    void closeSession_shouldInvokeService() throws Exception {
        mockMvc.perform(put("/api/admin/chat/session/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));
        verify(chatService, times(1)).closeSession(1L, "admin_close");
    }

    @Test
    @DisplayName("获取活跃会话数 - 返回 Map 包含 activeSessions/onlineAgents")
    void getActiveCount_shouldReturnMap() throws Exception {
        mockMvc.perform(get("/api/admin/chat/active-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.activeSessions", is(0)))
                .andExpect(jsonPath("$.data.onlineAgents", is(0)));
    }
}
