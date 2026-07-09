package com.example.minimall.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RAG 系统监控服务
 * 
 * 基于 Micrometer + Prometheus 的指标埋点体系
 * 
 * 核心指标：
 * 1. 检索响应时间（Timer）
 * 2. Embedding 生成耗时（Timer）
 * 3. LLM 调用成功率（Counter）
 * 4. 知识库命中率（Gauge + Counter）
 * 5. 查询意图分类统计
 * 6. 系统健康度评分
 */
@Service
public class RagMonitorService {
    private static final Logger logger = LoggerFactory.getLogger(RagMonitorService.class);
    
    private final MeterRegistry meterRegistry;
    
    // Micrometer 指标
    private final Timer retrievalTimer;
    private final Timer embeddingTimer;
    private final Timer llmCallTimer;
    private final Counter llmSuccessCounter;
    private final Counter llmFailureCounter;
    private final Counter ragHitCounter;
    private final Counter ragMissCounter;
    
    // 内存统计
    private final Map<String, AtomicLong> intentCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong totalRetrievals = new AtomicLong(0);
    private final AtomicLong totalRetrievalTimeMs = new AtomicLong(0);
    private final AtomicLong totalEmbeddingTimeMs = new AtomicLong(0);
    private final AtomicLong totalLlmCalls = new AtomicLong(0);
    private final AtomicLong totalLlmSuccess = new AtomicLong(0);
    private final AtomicLong totalRagHits = new AtomicLong(0);
    private final AtomicLong totalRagMisses = new AtomicLong(0);
    
    // 高频查询统计（最近1000条）
    private final Deque<String> recentQueries = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private static final int MAX_RECENT_QUERIES = 1000;
    
    @Autowired
    public RagMonitorService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 测试环境 MeterRegistry 可能为 null，需做空值检查
        if (meterRegistry != null) {
            // 初始化Timer
            this.retrievalTimer = Timer.builder("rag.retrieval.time")
                    .description("RAG检索响应时间")
                    .register(meterRegistry);
            this.embeddingTimer = Timer.builder("rag.embedding.time")
                    .description("Embedding生成耗时")
                    .register(meterRegistry);
            this.llmCallTimer = Timer.builder("rag.llm.call.time")
                    .description("LLM调用耗时")
                    .register(meterRegistry);
            
            // 初始化Counter
            this.llmSuccessCounter = Counter.builder("rag.llm.success")
                    .description("LLM调用成功次数")
                    .register(meterRegistry);
            this.llmFailureCounter = Counter.builder("rag.llm.failure")
                    .description("LLM调用失败次数")
                    .register(meterRegistry);
            this.ragHitCounter = Counter.builder("rag.retrieval.hit")
                    .description("RAG检索命中次数")
                    .register(meterRegistry);
            this.ragMissCounter = Counter.builder("rag.retrieval.miss")
                    .description("RAG检索未命中次数")
                    .register(meterRegistry);
            
            // 注册Gauge
            Gauge.builder("rag.hit.rate", this, svc -> svc.calculateHitRate())
                    .description("RAG知识库命中率")
                    .register(meterRegistry);
            Gauge.builder("rag.llm.success.rate", this, svc -> svc.calculateLlmSuccessRate())
                    .description("LLM调用成功率")
                    .register(meterRegistry);
            Gauge.builder("rag.health.score", this, svc -> svc.calculateHealthScore())
                    .description("系统健康度评分(0-100)")
                    .register(meterRegistry);
        } else {
            logger.warn("MeterRegistry 为 null，RAG 监控指标将不会注册到 Prometheus（通常出现在测试环境）");
            this.retrievalTimer = null;
            this.embeddingTimer = null;
            this.llmCallTimer = null;
            this.llmSuccessCounter = null;
            this.llmFailureCounter = null;
            this.ragHitCounter = null;
            this.ragMissCounter = null;
        }
    }
    
    /** 记录检索耗时 */
    public void recordRetrieval(long durationMs) {
        if (retrievalTimer != null) {
            retrievalTimer.record(java.time.Duration.ofMillis(durationMs));
        }
        totalRetrievals.incrementAndGet();
        totalRetrievalTimeMs.addAndGet(durationMs);
    }
    
    /** 记录Embedding耗时 */
    public void recordEmbedding(long durationMs) {
        if (embeddingTimer != null) {
            embeddingTimer.record(java.time.Duration.ofMillis(durationMs));
        }
        totalEmbeddingTimeMs.addAndGet(durationMs);
    }
    
    /** 记录LLM调用 */
    public void recordLlmCall(long durationMs, boolean success) {
        if (llmCallTimer != null) {
            llmCallTimer.record(java.time.Duration.ofMillis(durationMs));
        }
        totalLlmCalls.incrementAndGet();
        if (success) {
            if (llmSuccessCounter != null) {
                llmSuccessCounter.increment();
            }
            totalLlmSuccess.incrementAndGet();
        } else {
            if (llmFailureCounter != null) {
                llmFailureCounter.increment();
            }
        }
    }
    
    /** 记录RAG命中/未命中 */
    public void recordRagResult(boolean hit) {
        if (hit) {
            if (ragHitCounter != null) {
                ragHitCounter.increment();
            }
            totalRagHits.incrementAndGet();
        } else {
            if (ragMissCounter != null) {
                ragMissCounter.increment();
            }
            totalRagMisses.incrementAndGet();
        }
    }
    
    /** 记录查询意图 */
    public void recordIntent(String intent) {
        if (intent == null || intent.isEmpty()) {
            return;
        }
        intentCounts.computeIfAbsent(intent, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /** 记录用户查询 */
    public void recordQuery(String query) {
        if (query == null || query.isEmpty()) {
            return;
        }
        totalQueries.incrementAndGet();
        recentQueries.addLast(query);
        while (recentQueries.size() > MAX_RECENT_QUERIES) {
            recentQueries.pollFirst();
        }
    }
    
    /** 计算命中率 */
    public double calculateHitRate() {
        long hits = totalRagHits.get();
        long misses = totalRagMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    /** 计算LLM成功率 */
    public double calculateLlmSuccessRate() {
        long total = totalLlmCalls.get();
        return total > 0 ? (double) totalLlmSuccess.get() / total : 0.0;
    }
    
    /** 计算系统健康度评分(0-100) */
    public double calculateHealthScore() {
        // 健康度 = 命中率*30% + LLM成功率*40% + 响应速度分*30%
        double hitRate = calculateHitRate();
        double llmRate = calculateLlmSuccessRate();
        long retrievals = totalRetrievals.get();
        double avgRetrievalMs = retrievals > 0 ?
                (double) totalRetrievalTimeMs.get() / retrievals : 0;
        double speedScore = avgRetrievalMs > 0 ? Math.max(0, 1 - avgRetrievalMs / 1000.0) : 1.0;
        return (hitRate * 0.3 + llmRate * 0.4 + speedScore * 0.3) * 100;
    }
    
    /** 获取监控仪表盘数据 */
    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        
        // 基础计数
        dashboard.put("totalQueries", totalQueries.get());
        dashboard.put("totalLlmCalls", totalLlmCalls.get());
        dashboard.put("totalLlmSuccess", totalLlmSuccess.get());
        dashboard.put("totalRagHits", totalRagHits.get());
        dashboard.put("totalRagMisses", totalRagMisses.get());
        
        // 比率指标（保留2位小数）
        dashboard.put("hitRate", round2(calculateHitRate()));
        dashboard.put("llmSuccessRate", round2(calculateLlmSuccessRate()));
        dashboard.put("healthScore", round2(calculateHealthScore()));
        
        // 平均耗时（保留2位小数）
        long queries = totalQueries.get();
        long retrievals = totalRetrievals.get();
        double avgRetrievalMs = retrievals > 0 ? (double) totalRetrievalTimeMs.get() / retrievals : 0.0;
        double avgEmbeddingMs = queries > 0 ? (double) totalEmbeddingTimeMs.get() / queries : 0.0;
        dashboard.put("avgRetrievalTimeMs", round2(avgRetrievalMs));
        dashboard.put("avgEmbeddingTimeMs", round2(avgEmbeddingMs));
        
        // 意图分布
        Map<String, Object> intentDistribution = new LinkedHashMap<>();
        Map<String, AtomicLong> snapshot = new LinkedHashMap<>(intentCounts);
        long totalIntents = 0;
        for (AtomicLong cnt : snapshot.values()) {
            totalIntents += cnt.get();
        }
        for (Map.Entry<String, AtomicLong> entry : snapshot.entrySet()) {
            Map<String, Object> intentInfo = new LinkedHashMap<>();
            long count = entry.getValue().get();
            double percentage = totalIntents > 0 ? (double) count / totalIntents : 0.0;
            intentInfo.put("count", count);
            intentInfo.put("percentage", round2(percentage));
            intentDistribution.put(entry.getKey(), intentInfo);
        }
        dashboard.put("intentDistribution", intentDistribution);
        
        // 最近10条查询
        List<String> recent = new ArrayList<>();
        Iterator<String> descIter = recentQueries.descendingIterator();
        while (descIter.hasNext() && recent.size() < 10) {
            recent.add(descIter.next());
        }
        dashboard.put("recentQueries", recent);
        
        return dashboard;
    }
    
    /** 获取高频查询（简单实现：按完全匹配分组） */
    public List<Map<String, Object>> getTopQueries(int limit) {
        // 统计recentQueries中的高频查询
        Map<String, Long> counts = new HashMap<>();
        for (String q : recentQueries) {
            Long cur = counts.get(q);
            counts.put(q, cur == null ? 1L : cur + 1);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("query", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        // 按count降序排列，取前limit条
        Collections.sort(result, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                Long ca = (Long) a.get("count");
                Long cb = (Long) b.get("count");
                return cb.compareTo(ca);
            }
        });
        if (limit > 0 && result.size() > limit) {
            result = result.subList(0, limit);
        }
        return result;
    }
    
    /** 保留2位小数 */
    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
