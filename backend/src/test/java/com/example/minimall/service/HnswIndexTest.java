package com.example.minimall.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HNSW 索引单元测试（问题1：向量检索性能优化）
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>基本添加与搜索</li>
 *   <li>搜索结果排序正确性</li>
 *   <li>批量添加</li>
 *   <li>删除操作</li>
 *   <li>边界条件（空索引、单元素、维度不匹配）</li>
 *   <li>大规模数据性能验证</li>
 * </ul>
 * </p>
 */
class HnswIndexTest {

    private HnswIndex index;
    private static final int DIM = 128;
    private static final int M = 8;
    private static final int EF_CONSTRUCTION = 64;

    @BeforeEach
    void setUp() {
        index = new HnswIndex(DIM, M, EF_CONSTRUCTION);
    }

    /** 生成指定维度的随机向量 */
    private float[] randomVector(int dim) {
        Random rnd = new Random();
        float[] v = new float[dim];
        for (int i = 0; i < dim; i++) {
            v[i] = rnd.nextFloat();
        }
        return v;
    }

    /** 生成与目标向量接近的向量（添加少量噪声） */
    private float[] nearbyVector(float[] base, float noise) {
        Random rnd = new Random();
        float[] v = new float[base.length];
        for (int i = 0; i < base.length; i++) {
            v[i] = base[i] + (rnd.nextFloat() - 0.5f) * noise;
        }
        return v;
    }

    @Test
    void testAddAndSearchSingle() {
        float[] v1 = randomVector(DIM);
        index.add(1L, v1);

        List<HnswIndex.SearchResult> results = index.search(v1, 1, 32);
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).id);
        assertTrue(results.get(0).score > 0.99, "自己与自己的相似度应接近 1");
    }

    @Test
    void testSearchReturnsClosestFirst() {
        // 添加 100 个向量，验证搜索结果按相似度降序排列
        Map<Long, float[]> data = new HashMap<>();
        for (long i = 1; i <= 100; i++) {
            float[] v = randomVector(DIM);
            data.put(i, v);
            index.add(i, v);
        }

        // 用第 50 号向量作为查询
        float[] query = data.get(50L);
        List<HnswIndex.SearchResult> results = index.search(query, 5, 64);

        assertEquals(5, results.size());
        // 第一个结果应该是自己（相似度最高）
        assertEquals(50L, results.get(0).id);
        // 验证降序排列
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).score >= results.get(i).score,
                    "搜索结果应按相似度降序排列");
        }
    }

    @Test
    void testAddAll() {
        Map<Long, float[]> batch = new HashMap<>();
        for (long i = 1; i <= 50; i++) {
            batch.put(i, randomVector(DIM));
        }
        index.addAll(batch);

        assertEquals(50, index.size());

        // 验证可以搜索
        float[] query = batch.get(1L);
        List<HnswIndex.SearchResult> results = index.search(query, 3, 32);
        assertFalse(results.isEmpty());
        assertEquals(1L, results.get(0).id);
    }

    @Test
    void testRemove() {
        for (long i = 1; i <= 20; i++) {
            index.add(i, randomVector(DIM));
        }
        assertEquals(20, index.size());

        index.remove(10L);
        assertEquals(19, index.size());

        // 验证删除后仍可搜索
        float[] query = randomVector(DIM);
        List<HnswIndex.SearchResult> results = index.search(query, 5, 32);
        for (HnswIndex.SearchResult sr : results) {
            assertNotEquals(10L, sr.id, "已删除的节点不应出现在搜索结果中");
        }
    }

    @Test
    void testEmptyIndex() {
        List<HnswIndex.SearchResult> results = index.search(randomVector(DIM), 5, 32);
        assertTrue(results.isEmpty(), "空索引搜索应返回空列表");
    }

    @Test
    void testClear() {
        for (long i = 1; i <= 10; i++) {
            index.add(i, randomVector(DIM));
        }
        assertEquals(10, index.size());

        index.clear();
        assertEquals(0, index.size());

        List<HnswIndex.SearchResult> results = index.search(randomVector(DIM), 5, 32);
        assertTrue(results.isEmpty(), "清空后搜索应返回空列表");
    }

    @Test
    void testInvalidDimension() {
        assertThrows(IllegalArgumentException.class, () -> {
            index.add(1L, new float[DIM + 1]);
        }, "维度不匹配应抛出异常");
    }

    @Test
    void testInvalidConstructorParams() {
        assertThrows(IllegalArgumentException.class, () -> new HnswIndex(0, M, EF_CONSTRUCTION));
        assertThrows(IllegalArgumentException.class, () -> new HnswIndex(DIM, 1, EF_CONSTRUCTION));
        assertThrows(IllegalArgumentException.class, () -> new HnswIndex(DIM, M, 0));
    }

    @Test
    void testKZero() {
        index.add(1L, randomVector(DIM));
        List<HnswIndex.SearchResult> results = index.search(randomVector(DIM), 0, 32);
        assertTrue(results.isEmpty(), "k=0 应返回空列表");
    }

    @Test
    void testNullVector() {
        assertThrows(IllegalArgumentException.class, () -> index.add(1L, null));
        assertThrows(IllegalArgumentException.class, () -> index.search(null, 5, 32));
    }

    @Test
    void testLargeScalePerformance() {
        // 添加 5000 条数据，验证搜索性能 < 200ms（HNSW 目标）
        int scale = 5000;
        Map<Long, float[]> data = new HashMap<>(scale * 2);
        long addStart = System.currentTimeMillis();
        for (long i = 1; i <= scale; i++) {
            float[] v = randomVector(DIM);
            data.put(i, v);
            index.add(i, v);
        }
        long addTime = System.currentTimeMillis() - addStart;
        assertEquals(scale, index.size());

        // 随机选择一个向量作为查询
        long queryId = 2500L;
        float[] query = data.get(queryId);

        // 执行多次搜索取平均
        int iterations = 10;
        long totalSearchTime = 0;
        for (int i = 0; i < iterations; i++) {
            long searchStart = System.currentTimeMillis();
            List<HnswIndex.SearchResult> results = index.search(query, 10, 64);
            totalSearchTime += System.currentTimeMillis() - searchStart;
            assertFalse(results.isEmpty());
        }
        long avgSearchTime = totalSearchTime / iterations;

        // 验证查询结果包含查询向量自己（top-1 应该是自己）
        List<HnswIndex.SearchResult> finalResults = index.search(query, 1, 64);
        assertEquals(queryId, finalResults.get(0).id, "top-1 应该是查询向量自身");

        System.out.println("[HNSW性能] scale=" + scale + ", addTime=" + addTime + "ms, avgSearchTime=" + avgSearchTime + "ms");
        // 性能断言：平均搜索时间 < 200ms（HNSW 目标）
        assertTrue(avgSearchTime < 200, "HNSW 搜索平均耗时应 < 200ms, 实际=" + avgSearchTime + "ms");
    }

    @Test
    void testNearbyVectorsRankHigher() {
        // 添加 base 向量
        float[] base = randomVector(DIM);
        index.add(1L, base);

        // 添加一个接近 base 的向量
        float[] nearby = nearbyVector(base, 0.1f);
        index.add(2L, nearby);

        // 添加一个远离 base 的随机向量
        float[] far = randomVector(DIM);
        index.add(3L, far);

        // 用 base 搜索，2 号（nearby）应排在 3 号（far）前面
        List<HnswIndex.SearchResult> results = index.search(base, 3, 32);
        assertEquals(3, results.size());

        int rankNearby = -1, rankFar = -1;
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).id == 2L) rankNearby = i;
            if (results.get(i).id == 3L) rankFar = i;
        }
        assertTrue(rankNearby < rankFar, "近邻向量应排在远向量的前面");
    }
}
