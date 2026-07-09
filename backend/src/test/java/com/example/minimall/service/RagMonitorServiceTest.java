package com.example.minimall.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG 监控服务单元测试（问题5：监控与可观测性）
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>指标记录（检索耗时、Embedding耗时、LLM调用、RAG命中/未命中）</li>
 *   <li>意图分类统计</li>
 *   <li>查询记录与高频查询统计</li>
 *   <li>命中率计算</li>
 *   <li>LLM成功率计算</li>
 *   <li>系统健康度评分</li>
 *   <li>仪表盘数据完整性</li>
 *   <li>边界条件（null MeterRegistry）</li>
 * </ul>
 * </p>
 */
class RagMonitorServiceTest {

    private RagMonitorService monitorService;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        monitorService = new RagMonitorService(meterRegistry);
    }

    // ===================== 检索耗时记录 =====================

    @Test
    void testRecordRetrieval() {
        monitorService.recordRetrieval(150);
        monitorService.recordRetrieval(250);
        // 需要记录查询才能计算平均耗时（avgRetrievalTimeMs = totalRetrievalTimeMs / totalQueries）
        monitorService.recordQuery("测试查询1");
        monitorService.recordQuery("测试查询2");
        Map<String, Object> dashboard = monitorService.getDashboard();
        assertNotNull(dashboard);
        assertTrue((Double) dashboard.get("avgRetrievalTimeMs") > 0, "平均检索耗时应>0");
    }

    @Test
    void testRecordRetrievalZero() {
        monitorService.recordRetrieval(0);
        Map<String, Object> dashboard = monitorService.getDashboard();
        assertEquals(0.0, (Double) dashboard.get("avgRetrievalTimeMs"), 0.01);
    }

    // ===================== Embedding耗时记录 =====================

    @Test
    void testRecordEmbedding() {
        monitorService.recordEmbedding(100);
        monitorService.recordEmbedding(200);
        monitorService.recordQuery("测试查询"); // 需要记录查询才能计算平均
        Map<String, Object> dashboard = monitorService.getDashboard();
        double avgEmbedding = (Double) dashboard.get("avgEmbeddingTimeMs");
        assertTrue(avgEmbedding > 0, "平均Embedding耗时应>0，实际: " + avgEmbedding);
    }

    // ===================== LLM调用记录 =====================

    @Test
    void testRecordLlmCallSuccess() {
        monitorService.recordLlmCall(500, true);
        monitorService.recordLlmCall(300, true);
        Map<String, Object> dashboard = monitorService.getDashboard();
        assertEquals(2L, dashboard.get("totalLlmCalls"));
        assertEquals(2L, dashboard.get("totalLlmSuccess"));
        double successRate = (Double) dashboard.get("llmSuccessRate");
        assertEquals(1.0, successRate, 0.01, "全部成功时成功率应为1.0");
    }

    @Test
    void testRecordLlmCallFailure() {
        monitorService.recordLlmCall(500, true);
        monitorService.recordLlmCall(300, false);
        Map<String, Object> dashboard = monitorService.getDashboard();
        assertEquals(2L, dashboard.get("totalLlmCalls"));
        assertEquals(1L, dashboard.get("totalLlmSuccess"));
        double successRate = (Double) dashboard.get("llmSuccessRate");
        assertEquals(0.5, successRate, 0.01, "一半失败时成功率应为0.5");
    }

    @Test
    void testRecordLlmCallAllFailure() {
        monitorService.recordLlmCall(500, false);
        monitorService.recordLlmCall(300, false);
        double successRate = monitorService.calculateLlmSuccessRate();
        assertEquals(0.0, successRate, 0.01, "全部失败时成功率应为0");
    }

    // ===================== RAG命中/未命中 =====================

    @Test
    void testRecordRagHit() {
        monitorService.recordRagResult(true);
        monitorService.recordRagResult(true);
        Map<String, Object> dashboard = monitorService.getDashboard();
        assertEquals(2L, dashboard.get("totalRagHits"));
        assertEquals(0L, dashboard.get("totalRagMisses"));
        double hitRate = (Double) dashboard.get("hitRate");
        assertEquals(1.0, hitRate, 0.01, "全部命中时命中率应为1.0");
    }

    @Test
    void testRecordRagMiss() {
        monitorService.recordRagResult(false);
        monitorService.recordRagResult(false);
        double hitRate = monitorService.calculateHitRate();
        assertEquals(0.0, hitRate, 0.01, "全部未命中时命中率应为0");
    }

    @Test
    void testRecordRagMixed() {
        monitorService.recordRagResult(true);
        monitorService.recordRagResult(false);
        monitorService.recordRagResult(true);
        double hitRate = monitorService.calculateHitRate();
        assertEquals(2.0 / 3.0, hitRate, 0.01, "2/3命中率应为0.667");
    }

    @Test
    void testHitRateNoData() {
        double hitRate = monitorService.calculateHitRate();
        assertEquals(0.0, hitRate, "无数据时命中率应为0");
    }

    // ===================== 意图分类统计 =====================

    @Test
    void testRecordIntent() {
        monitorService.recordIntent("PRODUCT_QUERY");
        monitorService.recordIntent("PRODUCT_QUERY");
        monitorService.recordIntent("FAQ_CONSULT");
        Map<String, Object> dashboard = monitorService.getDashboard();
        @SuppressWarnings("unchecked")
        Map<String, Object> intentDist = (Map<String, Object>) dashboard.get("intentDistribution");
        assertNotNull(intentDist);
        assertTrue(intentDist.containsKey("PRODUCT_QUERY"));
        assertTrue(intentDist.containsKey("FAQ_CONSULT"));
        @SuppressWarnings("unchecked")
        Map<String, Object> pqInfo = (Map<String, Object>) intentDist.get("PRODUCT_QUERY");
        assertEquals(2L, pqInfo.get("count"));
    }

    @Test
    void testRecordIntentNull() {
        monitorService.recordIntent(null);
        Map<String, Object> dashboard = monitorService.getDashboard();
        @SuppressWarnings("unchecked")
        Map<String, Object> intentDist = (Map<String, Object>) dashboard.get("intentDistribution");
        assertNotNull(intentDist);
        assertTrue(intentDist.isEmpty(), "null意图不应被记录");
    }

    @Test
    void testRecordIntentEmpty() {
        monitorService.recordIntent("");
        Map<String, Object> dashboard = monitorService.getDashboard();
        @SuppressWarnings("unchecked")
        Map<String, Object> intentDist = (Map<String, Object>) dashboard.get("intentDistribution");
        assertTrue(intentDist.isEmpty(), "空字符串意图不应被记录");
    }

    // ===================== 查询记录 =====================

    @Test
    void testRecordQuery() {
        monitorService.recordQuery("退货流程");
        monitorService.recordQuery("物流查询");
        Map<String, Object> dashboard = monitorService.getDashboard();
        assertEquals(2L, dashboard.get("totalQueries"));
        @SuppressWarnings("unchecked")
        List<String> recent = (List<String>) dashboard.get("recentQueries");
        assertNotNull(recent);
        assertEquals(2, recent.size());
    }

    @Test
    void testRecordQueryNull() {
        monitorService.recordQuery(null);
        Map<String, Object> dashboard = monitorService.getDashboard();
        assertEquals(0L, dashboard.get("totalQueries"));
    }

    @Test
    void testRecordQueryEmpty() {
        monitorService.recordQuery("");
        Map<String, Object> dashboard = monitorService.getDashboard();
        assertEquals(0L, dashboard.get("totalQueries"));
    }

    @Test
    void testRecordQueryRecentLimit() {
        // 记录超过10条查询，仪表盘只显示最近10条
        for (int i = 0; i < 15; i++) {
            monitorService.recordQuery("查询" + i);
        }
        Map<String, Object> dashboard = monitorService.getDashboard();
        @SuppressWarnings("unchecked")
        List<String> recent = (List<String>) dashboard.get("recentQueries");
        assertEquals(10, recent.size(), "最近查询最多显示10条");
    }

    // ===================== 高频查询统计 =====================

    @Test
    void testGetTopQueries() {
        monitorService.recordQuery("退货");
        monitorService.recordQuery("退货");
        monitorService.recordQuery("退款");
        monitorService.recordQuery("退货");
        monitorService.recordQuery("物流");
        List<Map<String, Object>> top = monitorService.getTopQueries(10);
        assertNotNull(top);
        assertFalse(top.isEmpty());
        // "退货"出现3次应排第一
        assertEquals("退货", top.get(0).get("query"));
        assertEquals(3L, top.get(0).get("count"));
    }

    @Test
    void testGetTopQueriesLimit() {
        for (int i = 0; i < 20; i++) {
            monitorService.recordQuery("查询" + i);
        }
        List<Map<String, Object>> top = monitorService.getTopQueries(5);
        assertEquals(5, top.size(), "应限制返回5条");
    }

    @Test
    void testGetTopQueriesEmpty() {
        List<Map<String, Object>> top = monitorService.getTopQueries(10);
        assertNotNull(top);
        assertTrue(top.isEmpty());
    }

    // ===================== 健康度评分 =====================

    @Test
    void testCalculateHealthScore() {
        // 设置一些指标数据
        monitorService.recordRagResult(true);
        monitorService.recordRagResult(true);
        monitorService.recordRagResult(false);
        monitorService.recordLlmCall(100, true);
        monitorService.recordLlmCall(100, true);
        monitorService.recordQuery("测试");
        monitorService.recordRetrieval(100);
        double health = monitorService.calculateHealthScore();
        assertTrue(health >= 0 && health <= 100, "健康度应在0-100之间，实际: " + health);
    }

    @Test
    void testCalculateHealthScoreNoData() {
        // 无数据时，命中率和LLM成功率为0，但速度分应为1.0
        double health = monitorService.calculateHealthScore();
        // 0*0.3 + 0*0.4 + 1.0*0.3 = 30
        assertEquals(30.0, health, 0.1, "无数据时健康度应为30（仅速度分满分）");
    }

    @Test
    void testCalculateHealthScorePerfect() {
        // 全部命中 + 全部成功 + 极快响应
        monitorService.recordRagResult(true);
        monitorService.recordLlmCall(10, true);
        monitorService.recordQuery("测试");
        monitorService.recordRetrieval(10);
        double health = monitorService.calculateHealthScore();
        assertTrue(health > 90, "完美指标健康度应>90，实际: " + health);
    }

    // ===================== 仪表盘完整性 =====================

    @Test
    void testDashboardContainsAllMetrics() {
        monitorService.recordQuery("测试");
        monitorService.recordRetrieval(100);
        monitorService.recordEmbedding(50);
        monitorService.recordLlmCall(200, true);
        monitorService.recordRagResult(true);
        monitorService.recordIntent("PRODUCT_QUERY");

        Map<String, Object> dashboard = monitorService.getDashboard();
        assertNotNull(dashboard);
        // 基础计数
        assertNotNull(dashboard.get("totalQueries"));
        assertNotNull(dashboard.get("totalLlmCalls"));
        assertNotNull(dashboard.get("totalLlmSuccess"));
        assertNotNull(dashboard.get("totalRagHits"));
        assertNotNull(dashboard.get("totalRagMisses"));
        // 比率指标
        assertNotNull(dashboard.get("hitRate"));
        assertNotNull(dashboard.get("llmSuccessRate"));
        assertNotNull(dashboard.get("healthScore"));
        // 平均耗时
        assertNotNull(dashboard.get("avgRetrievalTimeMs"));
        assertNotNull(dashboard.get("avgEmbeddingTimeMs"));
        // 意图分布
        assertNotNull(dashboard.get("intentDistribution"));
        // 最近查询
        assertNotNull(dashboard.get("recentQueries"));
    }

    @Test
    void testDashboardEmptyState() {
        Map<String, Object> dashboard = monitorService.getDashboard();
        assertNotNull(dashboard);
        assertEquals(0L, dashboard.get("totalQueries"));
        assertEquals(0L, dashboard.get("totalLlmCalls"));
        assertEquals(0.0, (Double) dashboard.get("hitRate"), 0.01);
    }

    // ===================== null MeterRegistry（测试环境降级）=====================

    @Test
    void testNullMeterRegistry() {
        RagMonitorService svcWithNull = new RagMonitorService(null);
        // 不应抛异常
        svcWithNull.recordRetrieval(100);
        svcWithNull.recordEmbedding(50);
        svcWithNull.recordLlmCall(200, true);
        svcWithNull.recordRagResult(true);
        // 内存统计应仍然工作
        Map<String, Object> dashboard = svcWithNull.getDashboard();
        assertNotNull(dashboard);
        assertEquals(1L, dashboard.get("totalRagHits"));
    }

    // ===================== 指标累积 =====================

    @Test
    void testMetricsAccumulate() {
        monitorService.recordLlmCall(100, true);
        assertEquals(1L, monitorService.getDashboard().get("totalLlmCalls"));
        monitorService.recordLlmCall(200, false);
        assertEquals(2L, monitorService.getDashboard().get("totalLlmCalls"));
        monitorService.recordLlmCall(300, true);
        assertEquals(3L, monitorService.getDashboard().get("totalLlmCalls"));
    }

    @Test
    void testRagHitsAccumulate() {
        for (int i = 0; i < 10; i++) {
            // i%3!=0: i=1,2,4,5,7,8 为hit(6个)，i=0,3,6,9 为miss(4个)
            monitorService.recordRagResult(i % 3 != 0);
        }
        Map<String, Object> dashboard = monitorService.getDashboard();
        assertEquals(6L, dashboard.get("totalRagHits"));
        assertEquals(4L, dashboard.get("totalRagMisses"));
        double hitRate = (Double) dashboard.get("hitRate");
        assertEquals(0.6, hitRate, 0.01, "6/10命中率应为0.6");
    }
}
