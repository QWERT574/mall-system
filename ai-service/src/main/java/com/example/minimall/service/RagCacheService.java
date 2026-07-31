package com.example.minimall.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * RAG 语义缓存服务（P3-1）
 * <p>
 * 使用 Redis 存储查询向量 + 检索结果，对语义相似的查询直接返回缓存，
 * 避免重复执行完整的 RAG 管道，提升热点查询响应速度。
 * </p>
 * <p>
 * 缓存策略：
 * <ul>
 *   <li>缓存键：rag:cache:{uuid}</li>
 *   <li>缓存内容：查询向量 + 检索结果 JSON</li>
 *   <li>相似度阈值：0.95（高阈值确保语义一致性）</li>
 *   <li>TTL：2 小时</li>
 *   <li>最大缓存数：1000 条（LRU 淘汰）</li>
 * </ul>
 * </p>
 */
@Service
public class RagCacheService {
    private static final Logger logger = LoggerFactory.getLogger(RagCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CACHE_PREFIX = "rag:cache:";
    private static final String CACHE_INDEX_KEY = "rag:cache:index";
    private static final Duration CACHE_TTL = Duration.ofHours(2);
    private static final double SIMILARITY_THRESHOLD = 0.95;
    private static final int MAX_CACHE_SIZE = 1000;

    @Autowired(required = false)
    public RagCacheService(StringRedisTemplate redisTemplate, EmbeddingService embeddingService) {
        this.redisTemplate = redisTemplate;
        this.embeddingService = embeddingService;
    }

    /**
     * 尝试从缓存获取相似查询的检索结果
     *
     * @param query 用户查询
     * @return 缓存的检索结果，未命中返回 null
     */
    public RagService.RetrievalResult getCachedResult(String query) {
        if (redisTemplate == null || query == null || query.trim().isEmpty()) {
            return null;
        }

        try {
            float[] queryVector = embeddingService.embed(query);

            // 获取所有缓存键
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return null;
            }

            // 遍历查找相似查询
            for (String key : keys) {
                try {
                    Map<Object, Object> cacheData = redisTemplate.opsForHash().entries(key);
                    if (cacheData.isEmpty()) continue;

                    String vectorJson = (String) cacheData.get("vector");
                    if (vectorJson == null) continue;

                    float[] cachedVector = parseVector(vectorJson);
                    double similarity = EmbeddingService.cosineSimilarity(queryVector, cachedVector);

                    if (similarity >= SIMILARITY_THRESHOLD) {
                        String resultJson = (String) cacheData.get("result");
                        if (resultJson != null) {
                            RagService.RetrievalResult result = objectMapper.readValue(
                                    resultJson, RagService.RetrievalResult.class);
                            logger.info("[RAG缓存] 命中缓存: query='{}', similarity={}", 
                                    truncate(query, 30), String.format("%.4f", similarity));
                            return result;
                        }
                    }
                } catch (Exception e) {
                    logger.debug("[RAG缓存] 读取缓存条目失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("[RAG缓存] 查询缓存失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 缓存检索结果
     *
     * @param query  用户查询
     * @param result 检索结果
     */
    public void cacheResult(String query, RagService.RetrievalResult result) {
        if (redisTemplate == null || query == null || result == null) {
            return;
        }

        try {
            // 检查缓存数量，超限则清理
            cleanupIfNeeded();

            float[] queryVector = embeddingService.embed(query);
            String key = CACHE_PREFIX + UUID.randomUUID().toString().replace("-", "");

            Map<String, String> cacheData = new HashMap<>();
            cacheData.put("vector", serializeVector(queryVector));
            cacheData.put("result", objectMapper.writeValueAsString(result));
            cacheData.put("query", query);
            cacheData.put("timestamp", String.valueOf(System.currentTimeMillis()));

            redisTemplate.opsForHash().putAll(key, cacheData);
            redisTemplate.expire(key, CACHE_TTL);

            // 添加到索引集合
            redisTemplate.opsForSet().add(CACHE_INDEX_KEY, key);
            redisTemplate.expire(CACHE_INDEX_KEY, CACHE_TTL);

            logger.debug("[RAG缓存] 缓存结果: query='{}', key={}", truncate(query, 30), key);
        } catch (Exception e) {
            logger.warn("[RAG缓存] 缓存结果失败: {}", e.getMessage());
        }
    }

    /**
     * 清理过期缓存
     */
    public void cleanupIfNeeded() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && keys.size() > MAX_CACHE_SIZE) {
                // 简单 LRU：删除最早的缓存
                List<String> keyList = new ArrayList<>(keys);
                int toDelete = keyList.size() - MAX_CACHE_SIZE + 100; // 多删一些避免频繁清理
                for (int i = 0; i < toDelete && i < keyList.size(); i++) {
                    redisTemplate.delete(keyList.get(i));
                }
                logger.info("[RAG缓存] 清理 {} 条过期缓存", toDelete);
            }
        } catch (Exception e) {
            logger.warn("[RAG缓存] 清理缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        if (redisTemplate == null) return;
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            redisTemplate.delete(CACHE_INDEX_KEY);
            logger.info("[RAG缓存] 已清空所有缓存");
        } catch (Exception e) {
            logger.warn("[RAG缓存] 清空缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("enabled", redisTemplate != null);
        
        if (redisTemplate != null) {
            try {
                Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
                stats.put("cacheSize", keys != null ? keys.size() : 0);
                stats.put("maxCacheSize", MAX_CACHE_SIZE);
                stats.put("similarityThreshold", SIMILARITY_THRESHOLD);
                stats.put("ttlHours", CACHE_TTL.toHours());
            } catch (Exception e) {
                stats.put("error", e.getMessage());
            }
        }
        
        return stats;
    }

    // ==================== 序列化工具方法 ====================

    private String serializeVector(float[] vector) {
        if (vector == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] parseVector(String json) {
        if (json == null || json.isEmpty() || json.equals("[]")) {
            return new float[0];
        }
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception e) {
            return new float[0];
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
