package com.example.minimall.service;

import com.example.minimall.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品上下文优化器单元测试（问题6：商品上下文无限膨胀）
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>相关性排序（名称匹配、描述匹配、销量加权）</li>
 *   <li>Token 预算控制（控制在模型上下文40%以内）</li>
 *   <li>商品摘要生成</li>
 *   <li>边界条件（空列表、空查询、单商品）</li>
 *   <li>Token 估算</li>
 *   <li>优化统计信息</li>
 * </ul>
 * </p>
 */
class ProductContextOptimizerTest {

    private ProductContextOptimizer optimizer;

    @BeforeEach
    void setUp() {
        optimizer = new ProductContextOptimizer();
        // 注入 @Value 字段（模拟配置）
        ReflectionTestUtils.setField(optimizer, "maxContextTokens", 3000);
        ReflectionTestUtils.setField(optimizer, "maxItems", 15);
        ReflectionTestUtils.setField(optimizer, "maxDescLength", 60);
    }

    /** 创建测试商品 */
    private Product createProduct(Long id, String name, String desc, double price, int sales, int stock, Long categoryId) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(new BigDecimal(price));
        p.setSales(sales);
        p.setStock(stock);
        p.setCategoryId(categoryId);
        return p;
    }

    // ===================== 相关性排序 =====================

    @Test
    void testRankAndSelectNullProducts() {
        List<Product> result = optimizer.rankAndSelect(null, "苹果");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRankAndSelectEmptyProducts() {
        List<Product> result = optimizer.rankAndSelect(Collections.emptyList(), "苹果");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRankAndSelectNullQuery() {
        List<Product> products = Arrays.asList(
                createProduct(1L, "苹果", "新鲜", 10, 100, 50, 1L),
                createProduct(2L, "香蕉", "好吃", 8, 80, 40, 1L)
        );
        List<Product> result = optimizer.rankAndSelect(products, null);
        assertNotNull(result);
        assertEquals(2, result.size());
        // 查询为空时保持原顺序
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void testRankAndSelectEmptyQuery() {
        List<Product> products = Arrays.asList(
                createProduct(1L, "苹果", "新鲜", 10, 100, 50, 1L),
                createProduct(2L, "香蕉", "好吃", 8, 80, 40, 1L)
        );
        List<Product> result = optimizer.rankAndSelect(products, "  ");
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testRankAndSelectNameMatchPrioritized() {
        List<Product> products = Arrays.asList(
                createProduct(1L, "香蕉", "普通水果", 5, 10, 20, 1L),
                createProduct(2L, "红富士苹果", "新鲜水果", 10, 50, 30, 1L),
                createProduct(3L, "橙子", "柑橘类水果", 8, 30, 25, 1L)
        );
        List<Product> result = optimizer.rankAndSelect(products, "苹果");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 名称匹配的商品应排在第一位
        assertEquals(2L, result.get(0).getId(), "名称含'苹果'的商品应排第一");
    }

    @Test
    void testRankAndSelectDescMatch() {
        List<Product> products = Arrays.asList(
                createProduct(1L, "商品A", "普通商品", 5, 10, 20, 1L),
                createProduct(2L, "商品B", "新鲜苹果当季水果", 10, 50, 30, 1L)
        );
        List<Product> result = optimizer.rankAndSelect(products, "苹果");
        assertNotNull(result);
        // 描述含"苹果"的应排在前面
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void testRankAndSelectSalesWeighting() {
        // 两个商品名称都不匹配，但销量不同
        List<Product> products = Arrays.asList(
                createProduct(1L, "商品A", "描述A", 5, 10, 20, 1L),
                createProduct(2L, "商品B", "描述B", 10, 500, 30, 1L)
        );
        List<Product> result = optimizer.rankAndSelect(products, "蔬菜");
        assertNotNull(result);
        // 销量高的应排在前面
        assertEquals(2L, result.get(0).getId(), "销量高的商品应排前面");
    }

    @Test
    void testRankAndSelectMaxItemsLimit() {
        ReflectionTestUtils.setField(optimizer, "maxItems", 3);
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            products.add(createProduct((long) i, "商品" + i, "描述" + i, 10, 100 - i, 50, 1L));
        }
        List<Product> result = optimizer.rankAndSelect(products, "商品");
        assertNotNull(result);
        assertEquals(3, result.size(), "应截断到maxItems=3");
    }

    // ===================== 上下文构建 =====================

    @Test
    void testBuildOptimizedContextNull() {
        String context = optimizer.buildOptimizedContext(null);
        assertNotNull(context);
        assertTrue(context.contains("暂无"));
    }

    @Test
    void testBuildOptimizedContextEmpty() {
        String context = optimizer.buildOptimizedContext(Collections.emptyList());
        assertNotNull(context);
        assertTrue(context.contains("暂无"));
    }

    @Test
    void testBuildOptimizedContextWithProducts() {
        List<Product> products = Arrays.asList(
                createProduct(1L, "苹果", "新鲜苹果", 10.5, 100, 50, 1L),
                createProduct(2L, "香蕉", "海南香蕉", 8, 80, 40, 1L)
        );
        String context = optimizer.buildOptimizedContext(products);
        assertNotNull(context);
        assertTrue(context.contains("商品上下文"));
        assertTrue(context.contains("苹果"));
        assertTrue(context.contains("香蕉"));
        assertTrue(context.contains("¥10.5"));
        assertTrue(context.contains("销量"));
        assertTrue(context.contains("库存"));
    }

    @Test
    void testBuildOptimizedContextTokenBudgetControl() {
        // 验证上下文长度控制在预算内（40% * 3000 = 1200 tokens）
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            products.add(createProduct((long) i, "商品名称" + i,
                    "这是一个很长的商品描述用于测试token预算控制功能是否正常工作" + i,
                    10 + i, 100 - i, 50, (long) (i % 3)));
        }
        String context = optimizer.buildOptimizedContext(products);
        int tokens = optimizer.estimateTokens(context);
        int budget = (int) Math.round(3000 * 0.4);
        // 上下文 token 数应控制在预算内（允许少量尾部提示溢出）
        assertTrue(tokens <= budget + 50, "上下文token数应≤预算+50，实际: " + tokens + " 预算: " + budget);
    }

    @Test
    void testBuildOptimizedContextShowsCount() {
        List<Product> products = Arrays.asList(
                createProduct(1L, "苹果", "新鲜", 10, 100, 50, 1L),
                createProduct(2L, "香蕉", "好吃", 8, 80, 40, 1L)
        );
        String context = optimizer.buildOptimizedContext(products);
        assertTrue(context.contains("已展示 2/2"), "应展示商品数量统计");
    }

    @Test
    void testBuildOptimizedContextCategoryOverview() {
        List<Product> products = Arrays.asList(
                createProduct(1L, "苹果", "水果", 10, 100, 50, 1L),
                createProduct(2L, "香蕉", "水果", 8, 80, 40, 1L),
                createProduct(3L, "白菜", "蔬菜", 3, 60, 30, 2L)
        );
        String context = optimizer.buildOptimizedContext(products);
        assertTrue(context.contains("分类分布"), "应包含分类分布概览");
        assertTrue(context.contains("分类1"), "应包含分类1");
        assertTrue(context.contains("分类2"), "应包含分类2");
    }

    // ===================== Token 估算 =====================

    @Test
    void testEstimateTokensNull() {
        assertEquals(0, optimizer.estimateTokens(null));
    }

    @Test
    void testEstimateTokensEmpty() {
        assertEquals(0, optimizer.estimateTokens(""));
    }

    @Test
    void testEstimateTokensChinese() {
        int tokens = optimizer.estimateTokens("中文测试");
        // 4个中文 * 1.5 = 6 tokens
        assertEquals(6, tokens);
    }

    @Test
    void testEstimateTokensEnglish() {
        int tokens = optimizer.estimateTokens("hello");
        // 5个非中文字符 / 4 ≈ 1.25 → ceil = 2
        assertEquals(2, tokens);
    }

    @Test
    void testEstimateTokensMixed() {
        int tokens = optimizer.estimateTokens("hello中文test");
        // 2个中文(中、文) * 1.5 = 3 + 9个非中文(h,e,l,l,o,t,e,s,t) / 4 = 2.25 → ceil(5.25) = 6
        assertEquals(6, tokens);
    }

    // ===================== 商品摘要 =====================

    @Test
    void testProductSummaryTruncation() {
        ReflectionTestUtils.setField(optimizer, "maxDescLength", 10);
        String longDesc = "这是一个非常非常非常长的商品描述超过最大长度限制需要被截断处理的内容";
        List<Product> products = Collections.singletonList(
                createProduct(1L, "测试商品", longDesc, 10, 5, 10, 1L));
        String context = optimizer.buildOptimizedContext(products);
        // 描述应被截断
        assertFalse(context.contains(longDesc), "长描述应被截断");
        assertTrue(context.contains("..."), "截断后应有省略号");
    }

    @Test
    void testProductSummaryNullDesc() {
        List<Product> products = Collections.singletonList(
                createProduct(1L, "测试商品", null, 10, 5, 10, 1L));
        String context = optimizer.buildOptimizedContext(products);
        assertTrue(context.contains("无描述"), "无描述应显示'无描述'");
    }

    @Test
    void testProductSummaryNullPrice() {
        Product p = createProduct(1L, "测试商品", "描述", 10, 5, 10, 1L);
        p.setPrice(null);
        List<Product> products = Collections.singletonList(p);
        String context = optimizer.buildOptimizedContext(products);
        assertTrue(context.contains("¥0"), "价格为null应显示¥0");
    }

    // ===================== 优化统计 =====================

    @Test
    void testGetOptimizationStats() {
        List<Product> original = Arrays.asList(
                createProduct(1L, "苹果", "水果", 10, 100, 50, 1L),
                createProduct(2L, "香蕉", "水果", 8, 80, 40, 1L),
                createProduct(3L, "橙子", "水果", 6, 60, 30, 1L)
        );
        List<Product> optimized = original.subList(0, 2);
        String context = optimizer.buildOptimizedContext(optimized);
        Map<String, Object> stats = optimizer.getOptimizationStats(original, optimized, context);
        assertNotNull(stats);
        assertEquals(3, stats.get("originalCount"));
        assertEquals(2, stats.get("optimizedCount"));
        assertTrue((Integer) stats.get("contextLength") > 0);
        assertTrue((Integer) stats.get("estimatedTokens") > 0);
        assertEquals(1200, stats.get("tokenBudget")); // 3000 * 0.4
    }

    @Test
    void testGetOptimizationStatsNull() {
        Map<String, Object> stats = optimizer.getOptimizationStats(null, null, null);
        assertNotNull(stats);
        assertEquals(0, stats.get("originalCount"));
        assertEquals(0, stats.get("optimizedCount"));
        assertEquals(0, stats.get("contextLength"));
    }

    // ===================== 综合场景 =====================

    @Test
    void testFullOptimizationPipeline() {
        // 模拟完整的优化流程：排序 + 上下文构建
        List<Product> products = new ArrayList<>();
        products.add(createProduct(1L, "香蕉", "普通水果", 5, 10, 20, 1L));
        products.add(createProduct(2L, "苹果", "新鲜红富士苹果", 10, 200, 50, 1L));
        products.add(createProduct(3L, "橙子", "柑橘类水果", 8, 150, 30, 1L));
        products.add(createProduct(4L, "葡萄", "新鲜水果", 12, 180, 40, 1L));
        products.add(createProduct(5L, "白菜", "新鲜蔬菜", 3, 50, 60, 2L));

        // 排序
        List<Product> ranked = optimizer.rankAndSelect(products, "苹果");
        // 构建上下文
        String context = optimizer.buildOptimizedContext(ranked);
        // 统计
        Map<String, Object> stats = optimizer.getOptimizationStats(products, ranked, context);

        assertNotNull(ranked);
        assertNotNull(context);
        assertNotNull(stats);
        assertTrue(ranked.size() <= 15);
        assertTrue(context.contains("苹果"));
    }
}
