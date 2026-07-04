package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.mapper.AdminInterventionMapper;
import com.example.minimall.model.AdminIntervention;
import com.example.minimall.service.impl.AdminInterventionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInterventionServiceImplTest {

    @Mock
    private AdminInterventionMapper adminInterventionMapper;

    @InjectMocks
    private AdminInterventionServiceImpl adminInterventionService;

    private AdminIntervention testIntervention;

    @BeforeEach
    void setUp() {
        testIntervention = new AdminIntervention();
        testIntervention.setId(1L);
        testIntervention.setUserId(101L);
        testIntervention.setSellerId(11L);
        testIntervention.setOrderId(1001L);
        testIntervention.setIssueType("售后纠纷");
        testIntervention.setTitle("测试介入申请");
        testIntervention.setDescription("这是一个测试介入申请");
        testIntervention.setStatus(0);
        testIntervention.setCreatedAt(LocalDateTime.now());
        testIntervention.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建介入申请 - 应设置初始状态并插入")
    void createIntervention_shouldSetInitialStatusAndInsert() {
        when(adminInterventionMapper.insert(any(AdminIntervention.class))).thenReturn(1);

        AdminIntervention result = adminInterventionService.createIntervention(testIntervention);

        assertNotNull(result);
        assertEquals(0, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(adminInterventionMapper, times(1)).insert(any(AdminIntervention.class));
    }

    @Test
    @DisplayName("根据ID获取介入申请 - 存在时返回")
    void getInterventionById_whenExists_shouldReturn() {
        when(adminInterventionMapper.selectById(1L)).thenReturn(testIntervention);

        AdminIntervention result = adminInterventionService.getInterventionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(adminInterventionMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("根据ID获取介入申请 - 不存在时返回null")
    void getInterventionById_whenNotExists_shouldReturnNull() {
        when(adminInterventionMapper.selectById(999L)).thenReturn(null);

        AdminIntervention result = adminInterventionService.getInterventionById(999L);

        assertNull(result);
        verify(adminInterventionMapper, times(1)).selectById(999L);
    }

    @Test
    @DisplayName("根据用户ID获取介入申请列表")
    void getInterventionsByUserId_shouldReturnList() {
        when(adminInterventionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testIntervention));

        List<AdminIntervention> results = adminInterventionService.getInterventionsByUserId(101L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(101L, results.get(0).getUserId());
        verify(adminInterventionMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("根据商家ID获取介入申请列表")
    void getInterventionsBySellerId_shouldReturnList() {
        when(adminInterventionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testIntervention));

        List<AdminIntervention> results = adminInterventionService.getInterventionsBySellerId(11L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(11L, results.get(0).getSellerId());
        verify(adminInterventionMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询介入申请 - 不带状态过滤")
    void getInterventionsPage_withoutStatus_shouldReturnAll() {
        Page<AdminIntervention> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(testIntervention));
        page.setTotal(1);
        when(adminInterventionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        IPage<AdminIntervention> result = adminInterventionService.getInterventionsPage(1, 10, null);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(adminInterventionMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询介入申请 - 带状态过滤")
    void getInterventionsPage_withStatus_shouldFilterByStatus() {
        Page<AdminIntervention> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(testIntervention));
        page.setTotal(1);
        when(adminInterventionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        IPage<AdminIntervention> result = adminInterventionService.getInterventionsPage(1, 10, 0);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(adminInterventionMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("处理介入申请 - 成功处理并设置处理时间")
    void processIntervention_shouldUpdateStatusAndSetProcessedAt() {
        when(adminInterventionMapper.selectById(1L)).thenReturn(testIntervention);
        when(adminInterventionMapper.updateById(any(AdminIntervention.class))).thenReturn(1);

        AdminIntervention result = adminInterventionService.processIntervention(1L, 2, "已解决，退款处理", 1L);

        assertNotNull(result);
        assertEquals(2, result.getStatus());
        assertEquals(1L, result.getAdminId());
        assertEquals("已解决，退款处理", result.getAdminRemark());
        assertNotNull(result.getProcessedAt());
        verify(adminInterventionMapper, times(1)).selectById(1L);
        verify(adminInterventionMapper, times(1)).updateById(any(AdminIntervention.class));
    }

    @Test
    @DisplayName("处理介入申请 - 申请不存在时抛出异常")
    void processIntervention_whenNotExists_shouldThrowException() {
        when(adminInterventionMapper.selectById(999L)).thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class,
                () -> adminInterventionService.processIntervention(999L, 2, "test", 1L));
        assertEquals("介入申请不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新介入申请 - 成功更新部分字段")
    void updateIntervention_shouldUpdatePartialFields() {
        AdminIntervention existing = new AdminIntervention();
        existing.setId(1L);
        existing.setUserId(101L);
        existing.setTitle("原标题");
        existing.setDescription("原描述");
        existing.setIssueType("售后纠纷");
        existing.setStatus(0);

        AdminIntervention update = new AdminIntervention();
        update.setTitle("新标题");
        update.setDescription("新描述");

        when(adminInterventionMapper.selectById(1L)).thenReturn(existing);
        when(adminInterventionMapper.updateById(any(AdminIntervention.class))).thenReturn(1);

        AdminIntervention result = adminInterventionService.updateIntervention(1L, update);

        assertNotNull(result);
        assertEquals("新标题", result.getTitle());
        assertEquals("新描述", result.getDescription());
        assertEquals("售后纠纷", result.getIssueType());
        verify(adminInterventionMapper, times(1)).selectById(1L);
        verify(adminInterventionMapper, times(1)).updateById(any(AdminIntervention.class));
    }

    @Test
    @DisplayName("更新介入申请 - 不存在时抛出异常")
    void updateIntervention_whenNotExists_shouldThrowException() {
        when(adminInterventionMapper.selectById(999L)).thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class,
                () -> adminInterventionService.updateIntervention(999L, new AdminIntervention()));
        assertEquals("介入申请不存在", exception.getMessage());
    }

    @Test
    @DisplayName("删除介入申请")
    void deleteIntervention_shouldDeleteById() {
        when(adminInterventionMapper.deleteById(1L)).thenReturn(1);

        adminInterventionService.deleteIntervention(1L);

        verify(adminInterventionMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("获取待处理介入申请列表")
    void getPendingInterventions_shouldReturnPendingOnly() {
        testIntervention.setStatus(0);
        when(adminInterventionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testIntervention));

        List<AdminIntervention> results = adminInterventionService.getPendingInterventions();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(0, results.get(0).getStatus());
        verify(adminInterventionMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询待处理介入申请")
    void getPendingInterventionsPage_shouldReturnPagedPending() {
        Page<AdminIntervention> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(testIntervention));
        page.setTotal(1);
        when(adminInterventionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        IPage<AdminIntervention> result = adminInterventionService.getPendingInterventionsPage(1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(adminInterventionMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分配管理员 - 待处理状态自动转为处理中")
    void assignAdmin_whenPending_shouldChangeToProcessing() {
        testIntervention.setStatus(0);
        when(adminInterventionMapper.selectById(1L)).thenReturn(testIntervention);
        when(adminInterventionMapper.updateById(any(AdminIntervention.class))).thenReturn(1);

        AdminIntervention result = adminInterventionService.assignAdmin(1L, 5L);

        assertNotNull(result);
        assertEquals(5L, result.getAdminId());
        assertEquals(1, result.getStatus());
        verify(adminInterventionMapper, times(1)).selectById(1L);
        verify(adminInterventionMapper, times(1)).updateById(any(AdminIntervention.class));
    }

    @Test
    @DisplayName("分配管理员 - 申请不存在时抛出异常")
    void assignAdmin_whenNotExists_shouldThrowException() {
        when(adminInterventionMapper.selectById(999L)).thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class,
                () -> adminInterventionService.assignAdmin(999L, 5L));
        assertEquals("介入申请不存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取统计数据 - 应包含各状态计数")
    void getInterventionStats_shouldReturnAllCounts() {
        when(adminInterventionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L, 3L, 8L);

        Map<String, Object> stats = adminInterventionService.getInterventionStats();

        assertNotNull(stats);
        assertEquals(5L, stats.get("pending"));
        assertEquals(3L, stats.get("processing"));
        assertEquals(8L, stats.get("completed"));
        verify(adminInterventionMapper, times(3)).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取待处理数量")
    void getPendingCount_shouldReturnCount() {
        when(adminInterventionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        long count = adminInterventionService.getPendingCount();

        assertEquals(3L, count);
        verify(adminInterventionMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取处理中数量")
    void getProcessingCount_shouldReturnCount() {
        when(adminInterventionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        long count = adminInterventionService.getProcessingCount();

        assertEquals(2L, count);
        verify(adminInterventionMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取已完成数量 - 包含已解决和已关闭")
    void getCompletedCount_shouldIncludeResolvedAndClosed() {
        when(adminInterventionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(7L);

        long count = adminInterventionService.getCompletedCount();

        assertEquals(7L, count);
        verify(adminInterventionMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("完整介入流程 - 创建->分配->处理")
    void fullInterventionFlow_shouldSucceed() {
        when(adminInterventionMapper.insert(any(AdminIntervention.class))).thenReturn(1);
        AdminIntervention created = adminInterventionService.createIntervention(testIntervention);
        assertEquals(0, created.getStatus());

        when(adminInterventionMapper.selectById(1L)).thenReturn(created);
        when(adminInterventionMapper.updateById(any(AdminIntervention.class))).thenReturn(1);
        AdminIntervention assigned = adminInterventionService.assignAdmin(1L, 5L);
        assertEquals(1, assigned.getStatus());
        assertEquals(5L, assigned.getAdminId());

        when(adminInterventionMapper.selectById(1L)).thenReturn(assigned);
        AdminIntervention processed = adminInterventionService.processIntervention(1L, 2, "已处理完成", 5L);
        assertEquals(2, processed.getStatus());
        assertEquals("已处理完成", processed.getAdminRemark());
        assertNotNull(processed.getProcessedAt());
    }
}
