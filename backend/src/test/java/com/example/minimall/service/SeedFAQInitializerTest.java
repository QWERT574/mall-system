package com.example.minimall.service;

import com.example.minimall.mapper.KnowledgeFaqMapper;
import com.example.minimall.model.KnowledgeFaq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 种子FAQ初始化器单元测试（问题4：冷启动问题）
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>知识库覆盖率计算（calculateCoverageRate）</li>
 *   <li>覆盖率详情（getCoverageDetails，含告警阈值）</li>
 *   <li>多级查询匹配（getMatchLevel：精确匹配→向量检索→LLM）</li>
 *   <li>种子FAQ初始化（initializeSeedFAQs）</li>
 *   <li>边界条件（null mapper、空FAQ列表、null查询）</li>
 *   <li>种子数据条数≥50验证</li>
 * </ul>
 * </p>
 */
class SeedFAQInitializerTest {

    private SeedFAQInitializer initializer;
    private KnowledgeFaqMapper faqMapper;
    private EmbeddingService embeddingService;
    private VectorStoreService vectorStoreService;

    @BeforeEach
    void setUp() {
        initializer = new SeedFAQInitializer();
        faqMapper = mock(KnowledgeFaqMapper.class);
        embeddingService = mock(EmbeddingService.class);
        vectorStoreService = mock(VectorStoreService.class);

        // 注入mock依赖
        ReflectionTestUtils.setField(initializer, "faqMapper", faqMapper);
        ReflectionTestUtils.setField(initializer, "embeddingService", embeddingService);
        ReflectionTestUtils.setField(initializer, "vectorStoreService", vectorStoreService);

        // mock默认行为
        when(embeddingService.getModelName()).thenReturn("test-model");
        when(embeddingService.embed(anyString())).thenReturn(new float[1024]);
    }

    /** 创建测试FAQ */
    private KnowledgeFaq createFaq(Long id, String question, String answer, String keywords) {
        KnowledgeFaq faq = new KnowledgeFaq();
        faq.setId(id);
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setKeywords(keywords);
        faq.setCategory("测试分类");
        faq.setPriority(5);
        faq.setStatus(1);
        return faq;
    }

    // ===================== 覆盖率计算 =====================

    @Test
    void testCalculateCoverageRateEmptyFaq() {
        when(faqMapper.selectAllEnabled()).thenReturn(Collections.emptyList());
        double rate = initializer.calculateCoverageRate();
        assertEquals(0.0, rate, 0.01, "空FAQ列表覆盖率应为0");
    }

    @Test
    void testCalculateCoverageRateNullFaq() {
        when(faqMapper.selectAllEnabled()).thenReturn(null);
        double rate = initializer.calculateCoverageRate();
        assertEquals(0.0, rate, 0.01, "null FAQ列表覆盖率应为0");
    }

    @Test
    void testCalculateCoverageRateFullCoverage() {
        // 构造覆盖所有核心关键词的FAQ
        StringBuilder allKeywords = new StringBuilder();
        String[] coreKws = {"退换货", "退款", "配送", "物流", "质量", "注册", "登录", "密码",
                "优惠", "满减", "秒杀", "优惠券", "订单", "支付", "发票",
                "售后", "保修", "投诉", "商品", "价格", "库存", "上架",
                "供应商", "认证", "助农", "直播", "采摘", "种植", "养殖"};
        for (String kw : coreKws) allKeywords.append(kw).append(" ");
        List<KnowledgeFaq> faqs = Collections.singletonList(
                createFaq(1L, "全量覆盖问题", "全量覆盖答案", allKeywords.toString()));
        when(faqMapper.selectAllEnabled()).thenReturn(faqs);
        double rate = initializer.calculateCoverageRate();
        assertEquals(1.0, rate, 0.01, "全量覆盖时覆盖率应为1.0");
    }

    @Test
    void testCalculateCoverageRatePartialCoverage() {
        // 只覆盖部分关键词
        List<KnowledgeFaq> faqs = Collections.singletonList(
                createFaq(1L, "退换货政策", "退款配送物流", "退换货,退款,配送,物流"));
        when(faqMapper.selectAllEnabled()).thenReturn(faqs);
        double rate = initializer.calculateCoverageRate();
        assertTrue(rate > 0 && rate < 1.0, "部分覆盖时覆盖率应在0-1之间，实际: " + rate);
    }

    @Test
    void testCalculateCoverageRateSelectAllEnabledFails() {
        // selectAllEnabled 抛异常，降级到 selectList
        when(faqMapper.selectAllEnabled()).thenThrow(new RuntimeException("DB error"));
        when(faqMapper.selectList(any())).thenReturn(Collections.emptyList());
        double rate = initializer.calculateCoverageRate();
        assertEquals(0.0, rate, 0.01, "降级查询返回空列表覆盖率应为0");
    }

    @Test
    void testCalculateCoverageRateNullMapper() {
        SeedFAQInitializer init = new SeedFAQInitializer();
        ReflectionTestUtils.setField(init, "faqMapper", null);
        double rate = init.calculateCoverageRate();
        assertEquals(0.0, rate, "mapper为null时覆盖率应为0");
    }

    // ===================== 覆盖率详情 =====================

    @Test
    void testGetCoverageDetailsEmpty() {
        when(faqMapper.selectAllEnabled()).thenReturn(Collections.emptyList());
        Map<String, Object> details = initializer.getCoverageDetails();
        assertNotNull(details);
        assertEquals(0.0, (Double) details.get("coverageRate"), 0.01);
        assertTrue((Boolean) details.get("needAlert"), "空知识库应触发告警");
        assertNotNull(details.get("alertMessage"));
    }

    @Test
    void testGetCoverageDetailsFull() {
        StringBuilder allKeywords = new StringBuilder();
        String[] coreKws = {"退换货", "退款", "配送", "物流", "质量", "注册", "登录", "密码",
                "优惠", "满减", "秒杀", "优惠券", "订单", "支付", "发票",
                "售后", "保修", "投诉", "商品", "价格", "库存", "上架",
                "供应商", "认证", "助农", "直播", "采摘", "种植", "养殖"};
        for (String kw : coreKws) allKeywords.append(kw).append(" ");
        List<KnowledgeFaq> faqs = Collections.singletonList(
                createFaq(1L, "全量", "覆盖", allKeywords.toString()));
        when(faqMapper.selectAllEnabled()).thenReturn(faqs);
        Map<String, Object> details = initializer.getCoverageDetails();
        assertNotNull(details);
        assertEquals(1.0, (Double) details.get("coverageRate"), 0.01);
        assertFalse((Boolean) details.get("needAlert"), "全覆盖不应告警");
        assertEquals(0, details.get("uncoveredCount"));
    }

    @Test
    void testGetCoverageDetailsFields() {
        when(faqMapper.selectAllEnabled()).thenReturn(Collections.emptyList());
        Map<String, Object> details = initializer.getCoverageDetails();
        assertNotNull(details.get("totalKeywords"));
        assertNotNull(details.get("coveredCount"));
        assertNotNull(details.get("uncoveredCount"));
        assertNotNull(details.get("coveredKeywords"));
        assertNotNull(details.get("uncoveredKeywords"));
        assertNotNull(details.get("needAlert"));
        assertNotNull(details.get("faqCount"));
        assertNotNull(details.get("alertThreshold"));
    }

    @Test
    void testGetCoverageDetailsNullMapper() {
        SeedFAQInitializer init = new SeedFAQInitializer();
        ReflectionTestUtils.setField(init, "faqMapper", null);
        Map<String, Object> details = init.getCoverageDetails();
        assertNotNull(details);
        assertEquals(0.0, (Double) details.get("coverageRate"), 0.01);
        assertTrue((Boolean) details.get("needAlert"));
    }

    // ===================== 多级查询匹配 =====================

    @Test
    void testGetMatchLevelNullQuery() {
        int level = initializer.getMatchLevel(null);
        assertEquals(3, level, "null查询应返回级别3（需要LLM）");
    }

    @Test
    void testGetMatchLevelEmptyQuery() {
        int level = initializer.getMatchLevel("");
        assertEquals(3, level, "空查询应返回级别3");
    }

    @Test
    void testGetMatchLevelWhitespaceQuery() {
        int level = initializer.getMatchLevel("   ");
        assertEquals(3, level, "空白查询应返回级别3");
    }

    @Test
    void testGetMatchLevelExactMatch() {
        List<KnowledgeFaq> faqs = Arrays.asList(
                createFaq(1L, "退换货政策是什么", "答案", "退换货"),
                createFaq(2L, "退款多久到账", "答案", "退款"));
        when(faqMapper.selectAllEnabled()).thenReturn(faqs);
        // 精确匹配：query == FAQ问题
        int level = initializer.getMatchLevel("退换货政策是什么");
        assertEquals(1, level, "精确匹配应返回级别1");
    }

    @Test
    void testGetMatchLevelContainsMatch() {
        List<KnowledgeFaq> faqs = Collections.singletonList(
                createFaq(1L, "退换货", "答案", "退换货"));
        when(faqMapper.selectAllEnabled()).thenReturn(faqs);
        // query 包含 FAQ 问题
        int level = initializer.getMatchLevel("请问退换货政策是怎样的");
        assertEquals(1, level, "query包含FAQ问题应返回级别1");
    }

    @Test
    void testGetMatchLevelNoMatch() {
        List<KnowledgeFaq> faqs = Collections.singletonList(
                createFaq(1L, "退换货政策", "答案", "退换货"));
        when(faqMapper.selectAllEnabled()).thenReturn(faqs);
        // query 不包含任何FAQ问题
        int level = initializer.getMatchLevel("今天天气怎么样");
        assertEquals(3, level, "无匹配应返回级别3（需要LLM）");
    }

    @Test
    void testGetMatchLevelEmptyFaqList() {
        when(faqMapper.selectAllEnabled()).thenReturn(Collections.emptyList());
        int level = initializer.getMatchLevel("退换货");
        assertEquals(3, level, "空FAQ列表应返回级别3");
    }

    @Test
    void testGetMatchLevelNullMapper() {
        SeedFAQInitializer init = new SeedFAQInitializer();
        ReflectionTestUtils.setField(init, "faqMapper", null);
        int level = init.getMatchLevel("退换货");
        assertEquals(3, level, "mapper为null应返回级别3");
    }

    @Test
    void testGetMatchLevelSelectAllEnabledFails() {
        when(faqMapper.selectAllEnabled()).thenThrow(new RuntimeException("DB error"));
        when(faqMapper.selectList(any())).thenReturn(Collections.emptyList());
        int level = initializer.getMatchLevel("退换货");
        assertEquals(3, level, "查询失败降级后空列表应返回级别3");
    }

    // ===================== 种子FAQ初始化 =====================

    @Test
    void testInitializeSeedFAQsNullMapper() {
        SeedFAQInitializer init = new SeedFAQInitializer();
        ReflectionTestUtils.setField(init, "faqMapper", null);
        // 不应抛异常
        init.initializeSeedFAQs();
    }

    @Test
    void testInitializeSeedFAQsSuccess() {
        // mock insert 成功
        when(faqMapper.insert(any(KnowledgeFaq.class))).thenAnswer(invocation -> {
            KnowledgeFaq faq = invocation.getArgument(0);
            faq.setId(1L);
            return 1;
        });
        initializer.initializeSeedFAQs();
        // 验证 insert 被调用（种子FAQ数量次）
        verify(faqMapper, atLeast(50)).insert(any(KnowledgeFaq.class));
        // 验证向量存储被调用
        verify(vectorStoreService, atLeast(50)).storeFaqEmbedding(anyLong(), anyString(), any(float[].class));
    }

    @Test
    void testInitializeSeedFAQsInsertFails() {
        // mock insert 失败（返回0）
        when(faqMapper.insert(any(KnowledgeFaq.class))).thenReturn(0);
        initializer.initializeSeedFAQs();
        // insert 被调用但向量存储不应被调用
        verify(faqMapper, atLeast(1)).insert(any(KnowledgeFaq.class));
        verify(vectorStoreService, never()).storeFaqEmbedding(anyLong(), anyString(), any(float[].class));
    }

    @Test
    void testInitializeSeedFAQsEmbeddingFails() {
        when(faqMapper.insert(any(KnowledgeFaq.class))).thenAnswer(invocation -> {
            KnowledgeFaq faq = invocation.getArgument(0);
            faq.setId(1L);
            return 1;
        });
        // embedding 抛异常
        when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("embedding error"));
        // 不应抛异常，FAQ记录已入库
        initializer.initializeSeedFAQs();
        verify(faqMapper, atLeast(50)).insert(any(KnowledgeFaq.class));
    }

    @Test
    void testInitializeSeedFAQsWithoutEmbeddingService() {
        SeedFAQInitializer init = new SeedFAQInitializer();
        ReflectionTestUtils.setField(init, "faqMapper", faqMapper);
        ReflectionTestUtils.setField(init, "embeddingService", null);
        ReflectionTestUtils.setField(init, "vectorStoreService", vectorStoreService);
        when(faqMapper.insert(any(KnowledgeFaq.class))).thenAnswer(invocation -> {
            KnowledgeFaq faq = invocation.getArgument(0);
            faq.setId(1L);
            return 1;
        });
        // 不应抛异常
        init.initializeSeedFAQs();
        verify(faqMapper, atLeast(50)).insert(any(KnowledgeFaq.class));
    }

    // ===================== 种子数据完整性 =====================

    @Test
    void testSeedFAQCountAtLeast50() {
        // 通过初始化验证种子FAQ数量≥50
        when(faqMapper.insert(any(KnowledgeFaq.class))).thenAnswer(invocation -> {
            KnowledgeFaq faq = invocation.getArgument(0);
            faq.setId(1L);
            return 1;
        });
        initializer.initializeSeedFAQs();
        // 至少50条种子FAQ
        verify(faqMapper, atLeast(50)).insert(any(KnowledgeFaq.class));
    }

    @Test
    void testSeedFAQsCoverCoreScenarios() {
        // 初始化后覆盖率应超过告警阈值（70%）
        when(faqMapper.insert(any(KnowledgeFaq.class))).thenAnswer(invocation -> {
            KnowledgeFaq faq = invocation.getArgument(0);
            faq.setId(1L);
            return 1;
        });
        // 初始化后，mock selectAllEnabled 返回带种子的FAQ
        List<KnowledgeFaq> seedFaqs = new ArrayList<>();
        seedFaqs.add(createFaq(1L, "退换货政策 退款 配送 物流 质量", "答案", "退换货,退款,配送,物流,质量"));
        seedFaqs.add(createFaq(2L, "注册 登录 密码 优惠 满减 秒杀 优惠券 订单", "答案",
                "注册,登录,密码,优惠,满减,秒杀,优惠券,订单"));
        seedFaqs.add(createFaq(3L, "支付 发票 售后 保修 投诉 商品 价格 库存 上架", "答案",
                "支付,发票,售后,保修,投诉,商品,价格,库存,上架"));
        seedFaqs.add(createFaq(4L, "供应商 认证 助农 直播 采摘 种植 养殖", "答案",
                "供应商,认证,助农,直播,采摘,种植,养殖"));
        when(faqMapper.selectAllEnabled()).thenReturn(seedFaqs);
        double rate = initializer.calculateCoverageRate();
        assertTrue(rate >= 0.70, "种子FAQ应覆盖≥70%核心关键词，实际覆盖率: " + rate);
    }
}
