package com.example.minimall.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 意图识别与动态路由服务
 *
 * 支持5种核心意图分类：
 * 1. PRODUCT_QUERY - 商品查询（推荐、价格、分类）
 * 2. FAQ_CONSULT - FAQ咨询（售后政策、配送规则）
 * 3. TECH_SUPPORT - 技术支持（种植、养殖技术）
 * 4. COMPLAINT - 投诉建议
 * 5. CHITCHAT - 闲聊
 *
 * 采用规则引擎+关键词加权方案，目标准确率≥85%
 */
@Service
public class IntentClassifierService {
    private static final Logger logger = LoggerFactory.getLogger(IntentClassifierService.class);

    public enum Intent {
        PRODUCT_QUERY("商品查询", 1),
        FAQ_CONSULT("FAQ咨询", 3),
        TECH_SUPPORT("技术支持", 1),
        COMPLAINT("投诉建议", 3),
        CHITCHAT("闲聊", 1);

        public final String label;
        public final int defaultServiceType;

        Intent(String label, int defaultServiceType) {
            this.label = label;
            this.defaultServiceType = defaultServiceType;
        }
    }

    public static class ClassificationResult {
        public final Intent intent;
        public final double confidence;
        public final Map<Intent, Double> scores;
        public final String matchedRule;

        public ClassificationResult(Intent intent, double confidence,
                                    Map<Intent, Double> scores, String matchedRule) {
            this.intent = intent;
            this.confidence = confidence;
            this.scores = scores;
            this.matchedRule = matchedRule;
        }
    }

    // 意图关键词权重表
    private static final Map<Intent, Map<String, Double>> INTENT_KEYWORDS = new EnumMap<>(Intent.class);
    static {
        // 商品查询：推荐/价格/买/卖/商品/农产品等
        Map<String, Double> productKw = new HashMap<>();
        productKw.put("推荐", 3.0); productKw.put("商品", 2.5); productKw.put("价格", 2.0);
        productKw.put("买", 2.0); productKw.put("农产品", 2.5); productKw.put("水果", 2.0);
        productKw.put("蔬菜", 2.0); productKw.put("多少钱", 3.0); productKw.put("便宜", 1.5);
        productKw.put("新鲜", 1.5); productKw.put("特产", 2.0); productKw.put("有什么", 1.5);
        productKw.put("上市", 1.5); productKw.put("应季", 2.0); productKw.put("热销", 2.0);
        productKw.put("爆款", 2.0); productKw.put("库存", 1.5);
        INTENT_KEYWORDS.put(Intent.PRODUCT_QUERY, productKw);

        // FAQ咨询：售后/退换货/物流/配送/发票等
        Map<String, Double> faqKw = new HashMap<>();
        faqKw.put("售后", 3.5); faqKw.put("退换货", 3.5); faqKw.put("退款", 3.5);
        faqKw.put("物流", 3.0); faqKw.put("快递", 2.5); faqKw.put("配送", 2.5);
        faqKw.put("发票", 3.0); faqKw.put("政策", 2.5); faqKw.put("规则", 2.5);
        faqKw.put("运费", 2.5); faqKw.put("包邮", 2.0); faqKw.put("发货", 2.5);
        faqKw.put("到货", 2.0); faqKw.put("签收", 2.5); faqKw.put("保修", 3.0);
        faqKw.put("质量", 2.0); faqKw.put("问题", 1.5); faqKw.put("怎么办", 2.0);
        faqKw.put("如何", 1.0); faqKw.put("流程", 2.0);
        INTENT_KEYWORDS.put(Intent.FAQ_CONSULT, faqKw);

        // 技术支持：种植/养殖/病虫害/农药/肥料等
        Map<String, Double> techKw = new HashMap<>();
        techKw.put("种植", 3.5); techKw.put("养殖", 3.5); techKw.put("病虫害", 3.5);
        techKw.put("农药", 3.0); techKw.put("肥料", 3.0); techKw.put("栽培", 3.0);
        techKw.put("技术", 2.5); techKw.put("农业", 2.5); techKw.put("施肥", 3.0);
        techKw.put("灌溉", 2.5); techKw.put("土壤", 2.5); techKw.put("大棚", 2.5);
        techKw.put("育苗", 3.0); techKw.put("嫁接", 3.0); techKw.put("防虫", 3.0);
        techKw.put("除草", 2.5); techKw.put("收割", 2.5); techKw.put("储存", 2.0);
        INTENT_KEYWORDS.put(Intent.TECH_SUPPORT, techKw);

        // 投诉建议：投诉/建议/不满/差评等
        Map<String, Double> complaintKw = new HashMap<>();
        complaintKw.put("投诉", 4.0); complaintKw.put("建议", 2.5); complaintKw.put("不满", 3.5);
        complaintKw.put("差评", 3.5); complaintKw.put("举报", 4.0); complaintKw.put("骗子", 3.5);
        complaintKw.put("虚假", 3.0); complaintKw.put("欺骗", 3.5); complaintKw.put("态度", 2.5);
        complaintKw.put("服务差", 3.0); complaintKw.put("联系客服", 2.0);
        INTENT_KEYWORDS.put(Intent.COMPLAINT, complaintKw);

        // 闲聊：你好/谢谢/再见/天气等
        Map<String, Double> chatKw = new HashMap<>();
        chatKw.put("你好", 3.0); chatKw.put("您好", 3.0); chatKw.put("谢谢", 3.0);
        chatKw.put("再见", 3.0); chatKw.put("天气", 2.5); chatKw.put("你是谁", 3.5);
        chatKw.put("你是机器人", 3.5); chatKw.put("能做什么", 2.5); chatKw.put("名字", 2.0);
        chatKw.put("聊天", 2.5); chatKw.put("无聊", 2.0); chatKw.put("开心", 2.0);
        chatKw.put("生日快乐", 3.0); chatKw.put("早上好", 3.0); chatKw.put("晚上好", 3.0);
        INTENT_KEYWORDS.put(Intent.CHITCHAT, chatKw);
    }

    // 意图统计
    private final Map<Intent, AtomicLong> intentCounts = new EnumMap<>(Intent.class);
    private final AtomicLong totalClassifications = new AtomicLong(0);

    public IntentClassifierService() {
        for (Intent intent : Intent.values()) {
            intentCounts.put(intent, new AtomicLong(0));
        }
    }

    /**
     * 对用户查询进行意图分类
     *
     * @param query 用户查询文本
     * @return 分类结果，包含意图、置信度、各意图得分和匹配规则
     */
    public ClassificationResult classify(String query) {
        // 处理空查询，默认归为闲聊
        if (query == null || query.trim().isEmpty()) {
            Map<Intent, Double> emptyScores = new EnumMap<>(Intent.class);
            for (Intent intent : Intent.values()) {
                emptyScores.put(intent, 0.0);
            }
            incrementStatistics(Intent.CHITCHAT);
            logger.debug("[意图分类] 空查询，默认分类为 CHITCHAT");
            return new ClassificationResult(Intent.CHITCHAT, 0.3, emptyScores, "empty_query_default");
        }

        // 1. 对query进行toLowerCase
        String lowerQuery = query.toLowerCase();

        // 2. 遍历每个意图的关键词表，计算匹配得分
        Map<Intent, Double> scores = new EnumMap<>(Intent.class);
        Map<Intent, List<String>> matchedKeywords = new EnumMap<>(Intent.class);

        for (Intent intent : Intent.values()) {
            scores.put(intent, 0.0);
            matchedKeywords.put(intent, new ArrayList<>());
        }

        for (Map.Entry<Intent, Map<String, Double>> entry : INTENT_KEYWORDS.entrySet()) {
            Intent intent = entry.getKey();
            Map<String, Double> keywords = entry.getValue();
            double totalScore = 0.0;
            for (Map.Entry<String, Double> kwEntry : keywords.entrySet()) {
                String keyword = kwEntry.getKey();
                Double weight = kwEntry.getValue();
                // 关键词统一转小写后比较，避免大小写差异漏匹配
                if (lowerQuery.contains(keyword.toLowerCase())) {
                    totalScore += weight;
                    matchedKeywords.get(intent).add(keyword);
                }
            }
            scores.put(intent, totalScore);
        }

        // 3. 找到最高分和次高分的意图
        Intent topIntent = null;
        Intent secondIntent = null;
        double topScore = 0.0;
        double secondScore = 0.0;

        for (Intent intent : Intent.values()) {
            double score = scores.get(intent);
            if (score > topScore) {
                secondScore = topScore;
                secondIntent = topIntent;
                topScore = score;
                topIntent = intent;
            } else if (score > secondScore) {
                secondScore = score;
                secondIntent = intent;
            }
        }

        // 4. 计算置信度
        double confidence;
        String matchedRule;

        if (topScore == 0.0) {
            // 如果最高分为0，返回CHITCHAT置信度0.3
            incrementStatistics(Intent.CHITCHAT);
            matchedRule = "no_keyword_match_default_chitchat";
            logger.debug("[意图分类] 查询 '{}' 未匹配到任何关键词，默认分类为 CHITCHAT", query);
            return new ClassificationResult(Intent.CHITCHAT, 0.3, scores, matchedRule);
        }

        // 置信度 = 最高分 / (最高分 + 次高分)
        // 当次高分为0时，置信度为1.0
        if (secondScore == 0.0) {
            confidence = 1.0;
        } else {
            confidence = topScore / (topScore + secondScore);
        }

        // 5. 记录匹配到的规则
        List<String> topMatchedKws = matchedKeywords.get(topIntent);
        if (topMatchedKws != null && !topMatchedKws.isEmpty()) {
            matchedRule = topIntent.name() + ":" + String.join(",", topMatchedKws);
        } else {
            matchedRule = topIntent.name() + ":unknown";
        }

        // 6. 更新统计计数
        incrementStatistics(topIntent);

        logger.info("[意图分类] query='{}', intent={}, confidence={}, topScore={}, secondScore={}, matchedRule={}",
                query, topIntent.label, String.format("%.4f", confidence),
                topScore, secondScore, matchedRule);

        // 7. 返回ClassificationResult
        return new ClassificationResult(topIntent, confidence, scores, matchedRule);
    }

    /**
     * 更新意图统计计数
     */
    private void incrementStatistics(Intent intent) {
        AtomicLong counter = intentCounts.get(intent);
        if (counter != null) {
            counter.incrementAndGet();
        }
        totalClassifications.incrementAndGet();
    }

    /**
     * 根据意图获取推荐的serviceType
     */
    public int getServiceType(Intent intent) {
        if (intent == null) {
            return 1;
        }
        return intent.defaultServiceType;
    }

    /**
     * 判断是否应该使用RAG
     * FAQ_CONSULT和TECH_SUPPORT应该使用RAG
     * CHITCHAT不需要RAG
     * PRODUCT_QUERY可选使用RAG
     */
    public boolean shouldUseRag(Intent intent) {
        if (intent == null) {
            return false;
        }
        switch (intent) {
            case FAQ_CONSULT:
            case TECH_SUPPORT:
            case PRODUCT_QUERY:
                return true;
            case COMPLAINT:
            case CHITCHAT:
            default:
                return false;
        }
    }

    /**
     * 判断是否需要商品上下文
     */
    public boolean needsProductContext(Intent intent) {
        if (intent == null) {
            return false;
        }
        return intent == Intent.PRODUCT_QUERY;
    }

    /**
     * 获取意图分类统计
     *
     * @return 包含total、各意图count和percentage的Map
     */
    public Map<String, Object> getIntentStatistics() {
        Map<String, Object> statistics = new LinkedHashMap<>();
        long total = totalClassifications.get();
        statistics.put("total", total);

        Map<String, Object> intentStats = new LinkedHashMap<>();
        for (Intent intent : Intent.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            long count = intentCounts.get(intent) != null ? intentCounts.get(intent).get() : 0L;
            double percentage = total > 0 ? (count * 100.0) / total : 0.0;
            item.put("count", count);
            item.put("percentage", Math.round(percentage * 100.0) / 100.0);
            item.put("label", intent.label);
            intentStats.put(intent.name(), item);
        }
        statistics.put("intents", intentStats);

        return statistics;
    }
}
