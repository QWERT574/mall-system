package com.example.minimall.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 意图识别服务单元测试（问题7：意图识别路由）
 */
class IntentClassifierServiceTest {

    private IntentClassifierService classifier;

    @BeforeEach
    void setUp() {
        classifier = new IntentClassifierService();
    }

    @Test
    void testProductQueryIntent() {
        IntentClassifierService.ClassificationResult result = classifier.classify("推荐一些新鲜水果");
        assertNotNull(result);
        assertEquals(IntentClassifierService.Intent.PRODUCT_QUERY, result.intent);
        assertTrue(result.confidence > 0.5, "商品查询置信度应 > 0.5");
        assertFalse(result.scores.isEmpty());
    }

    @Test
    void testFaqConsultIntent() {
        IntentClassifierService.ClassificationResult result = classifier.classify("怎么退款？售后政策是什么");
        assertNotNull(result);
        assertEquals(IntentClassifierService.Intent.FAQ_CONSULT, result.intent);
        assertTrue(result.confidence > 0.5, "FAQ咨询置信度应 > 0.5");
    }

    @Test
    void testTechSupportIntent() {
        IntentClassifierService.ClassificationResult result = classifier.classify("水稻种植技术和病虫害防治");
        assertNotNull(result);
        assertEquals(IntentClassifierService.Intent.TECH_SUPPORT, result.intent);
        assertTrue(result.confidence > 0.5, "技术支持置信度应 > 0.5");
    }

    @Test
    void testComplaintIntent() {
        IntentClassifierService.ClassificationResult result = classifier.classify("我要投诉商家，服务太差了");
        assertNotNull(result);
        assertEquals(IntentClassifierService.Intent.COMPLAINT, result.intent);
        assertTrue(result.confidence > 0.5, "投诉意图置信度应 > 0.5");
    }

    @Test
    void testChitchatIntent() {
        IntentClassifierService.ClassificationResult result = classifier.classify("你好，你是谁");
        assertNotNull(result);
        assertEquals(IntentClassifierService.Intent.CHITCHAT, result.intent);
    }

    @Test
    void testEmptyQuery() {
        IntentClassifierService.ClassificationResult result = classifier.classify("");
        assertNotNull(result);
        assertEquals(IntentClassifierService.Intent.CHITCHAT, result.intent, "空查询默认归为闲聊");
        assertTrue(result.confidence < 0.5, "空查询置信度应较低");
    }

    @Test
    void testNullQuery() {
        IntentClassifierService.ClassificationResult result = classifier.classify(null);
        assertNotNull(result);
        assertEquals(IntentClassifierService.Intent.CHITCHAT, result.intent);
    }

    @Test
    void testGetServiceType() {
        assertEquals(1, classifier.getServiceType(IntentClassifierService.Intent.PRODUCT_QUERY));
        assertEquals(3, classifier.getServiceType(IntentClassifierService.Intent.FAQ_CONSULT));
        assertEquals(1, classifier.getServiceType(IntentClassifierService.Intent.TECH_SUPPORT));
        assertEquals(3, classifier.getServiceType(IntentClassifierService.Intent.COMPLAINT));
        assertEquals(1, classifier.getServiceType(IntentClassifierService.Intent.CHITCHAT));
        assertEquals(1, classifier.getServiceType(null));
    }

    @Test
    void testShouldUseRag() {
        assertTrue(classifier.shouldUseRag(IntentClassifierService.Intent.PRODUCT_QUERY));
        assertTrue(classifier.shouldUseRag(IntentClassifierService.Intent.FAQ_CONSULT));
        assertTrue(classifier.shouldUseRag(IntentClassifierService.Intent.TECH_SUPPORT));
        assertFalse(classifier.shouldUseRag(IntentClassifierService.Intent.COMPLAINT));
        assertFalse(classifier.shouldUseRag(IntentClassifierService.Intent.CHITCHAT));
        assertFalse(classifier.shouldUseRag(null));
    }

    @Test
    void testNeedsProductContext() {
        assertTrue(classifier.needsProductContext(IntentClassifierService.Intent.PRODUCT_QUERY));
        assertFalse(classifier.needsProductContext(IntentClassifierService.Intent.FAQ_CONSULT));
        assertFalse(classifier.needsProductContext(IntentClassifierService.Intent.CHITCHAT));
        assertFalse(classifier.needsProductContext(null));
    }

    @Test
    void testGetIntentStatistics() {
        // 分类几次
        classifier.classify("推荐水果");
        classifier.classify("怎么退款");
        classifier.classify("推荐蔬菜");
        classifier.classify("你好");

        Map<String, Object> stats = classifier.getIntentStatistics();
        assertNotNull(stats);
        assertEquals(4L, stats.get("total"));

        @SuppressWarnings("unchecked")
        Map<String, Object> intents = (Map<String, Object>) stats.get("intents");
        assertNotNull(intents);

        @SuppressWarnings("unchecked")
        Map<String, Object> productStats = (Map<String, Object>) intents.get("PRODUCT_QUERY");
        assertNotNull(productStats);
        assertEquals(2L, productStats.get("count"));
    }

    @Test
    void testScoresMapCompleteness() {
        IntentClassifierService.ClassificationResult result = classifier.classify("推荐商品");
        assertNotNull(result.scores);
        assertEquals(5, result.scores.size(), "应包含全部5种意图的得分");
        for (IntentClassifierService.Intent intent : IntentClassifierService.Intent.values()) {
            assertTrue(result.scores.containsKey(intent), "得分表应包含: " + intent);
        }
    }

    @Test
    void testNoKeywordMatch() {
        // 一个不匹配任何关键词的查询
        IntentClassifierService.ClassificationResult result = classifier.classify("xyzabc123");
        assertNotNull(result);
        assertEquals(IntentClassifierService.Intent.CHITCHAT, result.intent, "无匹配应默认归为闲聊");
        assertTrue(result.confidence < 0.5);
    }

    @Test
    void testMatchedRuleNotNull() {
        IntentClassifierService.ClassificationResult result = classifier.classify("推荐水果");
        assertNotNull(result.matchedRule);
        assertTrue(result.matchedRule.contains("PRODUCT_QUERY"));
    }

    @Test
    void testMultipleClassificationAccuracy() {
        // 意图识别准确率测试（问题7目标 ≥85%）
        String[][] testCases = {
                {"推荐水果", "PRODUCT_QUERY"},
                {"有什么农产品", "PRODUCT_QUERY"},
                {"苹果多少钱", "PRODUCT_QUERY"},
                {"退款流程", "FAQ_CONSULT"},
                {"物流多久到", "FAQ_CONSULT"},
                {"售后政策", "FAQ_CONSULT"},
                {"种植技术", "TECH_SUPPORT"},
                {"病虫害防治", "TECH_SUPPORT"},
                {"我要投诉", "COMPLAINT"},
                {"差评", "COMPLAINT"},
                {"你好", "CHITCHAT"},
                {"谢谢", "CHITCHAT"}
        };

        int correct = 0;
        for (String[] tc : testCases) {
            IntentClassifierService.ClassificationResult result = classifier.classify(tc[0]);
            if (result.intent.name().equals(tc[1])) {
                correct++;
            } else {
                System.out.println("[意图识别错误] query='" + tc[0] + "', expected=" + tc[1] + ", actual=" + result.intent.name());
            }
        }
        double accuracy = (double) correct / testCases.length;
        System.out.println("[意图识别准确率] " + correct + "/" + testCases.length + " = " + (accuracy * 100) + "%");
        assertTrue(accuracy >= 0.85, "意图识别准确率应 ≥ 85%, 实际=" + (accuracy * 100) + "%");
    }
}
