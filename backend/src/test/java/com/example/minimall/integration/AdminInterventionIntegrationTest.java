package com.example.minimall.integration;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.minimall.common.Result;
import com.example.minimall.mapper.AdminInterventionMapper;
import com.example.minimall.model.AdminIntervention;
import com.example.minimall.service.AdminInterventionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.minimall.config.TestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestConfig.class)
class AdminInterventionIntegrationTest {

    @Autowired
    private AdminInterventionService adminInterventionService;

    @Autowired
    private AdminInterventionMapper adminInterventionMapper;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    private AdminIntervention testIntervention;

    @BeforeEach
    void setUp() {
        adminInterventionMapper.delete(null);

        testIntervention = new AdminIntervention();
        testIntervention.setUserId(101L);
        testIntervention.setSellerId(11L);
        testIntervention.setOrderId(1001L);
        testIntervention.setProductId(2001L);
        testIntervention.setIssueType("售后纠纷");
        testIntervention.setTitle("商品质量问题");
        testIntervention.setDescription("收到商品有瑕疵，要求退款");
        testIntervention.setEvidenceImages("http://example.com/img1.jpg");
        testIntervention.setStatus(0);
        testIntervention.setCreatedAt(LocalDateTime.now());
        testIntervention.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("完整介入流程：创建 → 查询 → 分配 → 处理")
    void fullInterventionFlow_shouldSucceed() {
        AdminIntervention created = adminInterventionService.createIntervention(testIntervention);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(0, created.getStatus());
        assertEquals("售后纠纷", created.getIssueType());
        assertEquals("商品质量问题", created.getTitle());

        AdminIntervention fetched = adminInterventionService.getInterventionById(created.getId());
        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals(101L, fetched.getUserId());

        List<AdminIntervention> userInterventions = adminInterventionService.getInterventionsByUserId(101L);
        assertNotNull(userInterventions);
        assertEquals(1, userInterventions.size());

        AdminIntervention assigned = adminInterventionService.assignAdmin(created.getId(), 5L);
        assertNotNull(assigned);
        assertEquals(1, assigned.getStatus());
        assertEquals(5L, assigned.getAdminId());

        AdminIntervention processed = adminInterventionService.processIntervention(
                created.getId(), 2, "已核实，同意退款", 5L);
        assertNotNull(processed);
        assertEquals(2, processed.getStatus());
        assertEquals("已核实，同意退款", processed.getAdminRemark());
        assertNotNull(processed.getProcessedAt());

        AdminIntervention finalCheck = adminInterventionService.getInterventionById(created.getId());
        assertEquals(2, finalCheck.getStatus());
        assertEquals(5L, finalCheck.getAdminId());
        assertNotNull(finalCheck.getProcessedAt());
    }

    @Test
    @DisplayName("待处理介入申请查询")
    void pendingInterventions_shouldReturnOnlyPending() {
        adminInterventionService.createIntervention(testIntervention);

        AdminIntervention resolved = new AdminIntervention();
        resolved.setUserId(102L);
        resolved.setSellerId(12L);
        resolved.setOrderId(1002L);
        resolved.setIssueType("商品投诉");
        resolved.setTitle("其他问题");
        resolved.setDescription("描述");
        resolved.setStatus(2);
        resolved.setAdminId(5L);
        resolved.setProcessedAt(LocalDateTime.now());
        resolved.setCreatedAt(LocalDateTime.now());
        resolved.setUpdatedAt(LocalDateTime.now());
        adminInterventionMapper.insert(resolved);

        List<AdminIntervention> pendingList = adminInterventionService.getPendingInterventions();
        assertNotNull(pendingList);
        assertEquals(1, pendingList.size());
        assertEquals(0, pendingList.get(0).getStatus());
    }

    @Test
    @DisplayName("分页查询 - 带状态过滤")
    void interventionsPage_withStatusFilter_shouldFilter() {
        adminInterventionService.createIntervention(testIntervention);

        IPage<AdminIntervention> page0 = adminInterventionService.getInterventionsPage(1, 10, 0);
        assertNotNull(page0);
        assertEquals(1, page0.getTotal());

        IPage<AdminIntervention> pendingPage = adminInterventionService.getPendingInterventionsPage(1, 10);
        assertNotNull(pendingPage);
        assertEquals(1, pendingPage.getTotal());

        IPage<AdminIntervention> pageAll = adminInterventionService.getInterventionsPage(1, 10, null);
        assertNotNull(pageAll);
        assertEquals(1, pageAll.getTotal());
    }

    @Test
    @DisplayName("统计数据 - 返回正确的各状态数量")
    void interventionStats_shouldReturnCorrectCounts() {
        adminInterventionService.createIntervention(testIntervention);

        Map<String, Object> stats = adminInterventionService.getInterventionStats();
        assertNotNull(stats);
        assertEquals(1L, stats.get("pending"));
        assertEquals(0L, stats.get("processing"));
        assertEquals(0L, stats.get("completed"));

        assertEquals(1L, adminInterventionService.getPendingCount());
        assertEquals(0L, adminInterventionService.getProcessingCount());
        assertEquals(0L, adminInterventionService.getCompletedCount());
    }

    @Test
    @DisplayName("同一用户多介入申请 - 返回全部")
    void multipleInterventionsForSameUser_shouldReturnAll() {
        adminInterventionService.createIntervention(testIntervention);

        AdminIntervention second = new AdminIntervention();
        second.setUserId(101L);
        second.setSellerId(11L);
        second.setOrderId(1002L);
        second.setIssueType("订单问题");
        second.setTitle("订单未发货");
        second.setDescription("已付款但未发货");
        second.setStatus(0);
        second.setCreatedAt(LocalDateTime.now());
        second.setUpdatedAt(LocalDateTime.now());
        adminInterventionService.createIntervention(second);

        List<AdminIntervention> userInterventions = adminInterventionService.getInterventionsByUserId(101L);
        assertNotNull(userInterventions);
        assertEquals(2, userInterventions.size());
    }

    @Test
    @DisplayName("更新介入申请 - 部分更新")
    void updateIntervention_shouldUpdatePartialFields() {
        AdminIntervention created = adminInterventionService.createIntervention(testIntervention);

        AdminIntervention update = new AdminIntervention();
        update.setTitle("更新后的标题");
        update.setDescription("更新后的描述");

        AdminIntervention updated = adminInterventionService.updateIntervention(created.getId(), update);
        assertNotNull(updated);
        assertEquals("更新后的标题", updated.getTitle());
        assertEquals("更新后的描述", updated.getDescription());
        assertEquals(created.getIssueType(), updated.getIssueType());
    }

    @Test
    @DisplayName("删除介入申请")
    void deleteIntervention_shouldRemove() {
        AdminIntervention created = adminInterventionService.createIntervention(testIntervention);
        assertNotNull(adminInterventionService.getInterventionById(created.getId()));

        adminInterventionService.deleteIntervention(created.getId());
        assertNull(adminInterventionService.getInterventionById(created.getId()));
    }

    @Test
    @DisplayName("分配管理员 - 更新adminId和状态")
    void assignAdmin_shouldUpdateAdminIdAndStatus() {
        AdminIntervention created = adminInterventionService.createIntervention(testIntervention);

        AdminIntervention assigned = adminInterventionService.assignAdmin(created.getId(), 10L);
        assertNotNull(assigned);
        assertEquals(10L, assigned.getAdminId());
        assertEquals(1, assigned.getStatus());

        AdminIntervention fetched = adminInterventionService.getInterventionById(created.getId());
        assertEquals(10L, fetched.getAdminId());
        assertEquals(1, fetched.getStatus());
    }

    @Test
    @DisplayName("创建介入申请 - 缺失必填字段应失败")
    void createIntervention_missingRequiredFields_shouldFail() {
        AdminIntervention invalid = new AdminIntervention();
        invalid.setUserId(101L);

        assertThrows(Exception.class, () -> adminInterventionService.createIntervention(invalid));
    }
}
