package com.example.minimall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.model.AdminIntervention;
import com.example.minimall.service.AdminInterventionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminInterventionControllerTest {

    @Mock
    private AdminInterventionService adminInterventionService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AdminInterventionController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AdminIntervention testIntervention;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testIntervention = new AdminIntervention();
        testIntervention.setId(1L);
        testIntervention.setUserId(101L);
        testIntervention.setSellerId(11L);
        testIntervention.setOrderId(1001L);
        testIntervention.setIssueType("售后纠纷");
        testIntervention.setTitle("测试介入");
        testIntervention.setDescription("测试描述");
        testIntervention.setStatus(0);
        testIntervention.setCreatedAt(LocalDateTime.now());
        testIntervention.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建介入申请 - 成功")
    void createIntervention_shouldSucceed() throws Exception {
        when(adminInterventionService.createIntervention(any(AdminIntervention.class)))
                .thenReturn(testIntervention);

        String requestBody = objectMapper.writeValueAsString(testIntervention);

        mockMvc.perform(post("/api/admin/intervention")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.userId", is(101)))
                .andExpect(jsonPath("$.data.status", is(0)));

        verify(adminInterventionService, times(1)).createIntervention(any(AdminIntervention.class));
    }

    @Test
    @DisplayName("创建介入申请 - 服务异常时返回错误")
    void createIntervention_whenServiceFails_shouldReturnError() throws Exception {
        when(adminInterventionService.createIntervention(any(AdminIntervention.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        String requestBody = objectMapper.writeValueAsString(testIntervention);

        mockMvc.perform(post("/api/admin/intervention")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)));
    }

    @Test
    @DisplayName("根据ID获取介入申请 - 存在时成功返回")
    void getInterventionById_whenExists_shouldReturn() throws Exception {
        when(adminInterventionService.getInterventionById(1L)).thenReturn(testIntervention);

        mockMvc.perform(get("/api/admin/intervention/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.issueType", is("售后纠纷")));

        verify(adminInterventionService, times(1)).getInterventionById(1L);
    }

    @Test
    @DisplayName("根据ID获取介入申请 - 不存在时返回错误")
    void getInterventionById_whenNotExists_shouldReturnError() throws Exception {
        when(adminInterventionService.getInterventionById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/admin/intervention/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)))
                .andExpect(jsonPath("$.message", is("申请不存在")));
    }

    @Test
    @DisplayName("根据用户ID获取介入申请列表")
    void getInterventionsByUserId_shouldReturnList() throws Exception {
        when(adminInterventionService.getInterventionsByUserId(101L))
                .thenReturn(Collections.singletonList(testIntervention));

        mockMvc.perform(get("/api/admin/intervention/user/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].userId", is(101)));
    }

    @Test
    @DisplayName("分页查询介入申请")
    void getInterventionsPage_shouldReturnPage() throws Exception {
        Page<AdminIntervention> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(testIntervention));
        page.setTotal(1);
        when(adminInterventionService.getInterventionsPage(eq(1), eq(10), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/intervention")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.total", is(1)))
                .andExpect(jsonPath("$.data.records", hasSize(1)));
    }

    @Test
    @DisplayName("分配管理员 - 成功")
    void assignAdmin_shouldSucceed() throws Exception {
        testIntervention.setAdminId(5L);
        testIntervention.setStatus(1);
        when(adminInterventionService.assignAdmin(1L, 5L)).thenReturn(testIntervention);

        Map<String, Object> request = new HashMap<>();
        request.put("adminId", 5);

        mockMvc.perform(post("/api/admin/intervention/1/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.adminId", is(5)));

        verify(adminInterventionService, times(1)).assignAdmin(1L, 5L);
    }

    @Test
    @DisplayName("处理介入申请 - 成功并发送WebSocket通知")
    void processIntervention_shouldSucceedAndSendNotification() throws Exception {
        testIntervention.setSessionId(100L);
        testIntervention.setStatus(2);
        testIntervention.setAdminId(1L);
        when(adminInterventionService.processIntervention(eq(1L), eq(2), anyString(), eq(1L)))
                .thenReturn(testIntervention);

        Map<String, Object> request = new HashMap<>();
        request.put("status", 2);
        request.put("remark", "已处理");
        request.put("adminId", 1);

        mockMvc.perform(post("/api/admin/intervention/1/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.status", is(2)));

        verify(adminInterventionService, times(1)).processIntervention(eq(1L), eq(2), anyString(), eq(1L));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat/user/" + 101L), any(Object.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat/seller/" + 11L), any(Object.class));
    }

    @Test
    @DisplayName("更新介入申请 - 成功")
    void updateIntervention_shouldSucceed() throws Exception {
        when(adminInterventionService.updateIntervention(eq(1L), any(AdminIntervention.class)))
                .thenReturn(testIntervention);

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("title", "更新标题");

        mockMvc.perform(put("/api/admin/intervention/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        verify(adminInterventionService, times(1)).updateIntervention(eq(1L), any(AdminIntervention.class));
    }

    @Test
    @DisplayName("删除介入申请 - 成功")
    void deleteIntervention_shouldSucceed() throws Exception {
        doNothing().when(adminInterventionService).deleteIntervention(1L);

        mockMvc.perform(delete("/api/admin/intervention/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)));

        verify(adminInterventionService, times(1)).deleteIntervention(1L);
    }

    @Test
    @DisplayName("获取统计数据 - 成功")
    void getStats_shouldSucceed() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", 5L);
        stats.put("processing", 3L);
        stats.put("completed", 8L);
        when(adminInterventionService.getInterventionStats()).thenReturn(stats);

        mockMvc.perform(post("/api/admin/intervention/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.pending", is(5)))
                .andExpect(jsonPath("$.data.processing", is(3)))
                .andExpect(jsonPath("$.data.completed", is(8)));
    }

    @Test
    @DisplayName("获取待处理介入申请 - 成功")
    void getPendingInterventions_shouldSucceed() throws Exception {
        Page<AdminIntervention> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(testIntervention));
        page.setTotal(1);
        when(adminInterventionService.getPendingInterventionsPage(eq(1), eq(10)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/intervention/pending")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data.total", is(1)))
                .andExpect(jsonPath("$.data.records[0].status", is(0)));
    }
}
