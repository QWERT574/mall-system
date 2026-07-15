package com.example.minimall.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感信息过滤服务单元测试（问题3：敏感信息过滤机制）
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>第1层：PII检测（手机号、邮箱、身份证号、银行卡号、IP）</li>
 *   <li>第2层：违禁词过滤</li>
 *   <li>第3层：敏感商业信息检测</li>
 *   <li>审计日志记录与查询</li>
 *   <li>边界条件（null、空字符串、无敏感内容）</li>
 *   <li>filterUserQuery / filterProductContext 入口方法</li>
 * </ul>
 * </p>
 */
class ContentFilterServiceTest {

    private ContentFilterService filterService;

    @BeforeEach
    void setUp() {
        filterService = new ContentFilterService();
    }

    // ===================== 第1层：PII检测 =====================

    @Test
    void testFilterPhoneNumber() {
        String content = "联系我手机号13812345678有问题";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertTrue(result.hasSensitiveContent);
        assertTrue(result.detectedTypes.contains("PII"));
        assertTrue(result.filteredContent.contains("[REDACTED_PHONE]"));
        assertFalse(result.filteredContent.contains("13812345678"));
        assertEquals(1, result.totalFiltered);
    }

    @Test
    void testFilterEmail() {
        String content = "发邮件到test@example.com咨询";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertTrue(result.hasSensitiveContent);
        assertTrue(result.filteredContent.contains("[REDACTED_EMAIL]"));
        assertFalse(result.filteredContent.contains("test@example.com"));
    }

    @Test
    void testFilterIdCard() {
        // 使用不含手机号子串（1[3-9]\d{9}）的身份证号
        String content = "身份证号110101200003071234";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertTrue(result.hasSensitiveContent);
        assertTrue(result.filteredContent.contains("[REDACTED_IDCARD]"));
        assertFalse(result.filteredContent.contains("110101200003071234"));
    }

    @Test
    void testFilterBankCard() {
        // 使用16位银行卡号（不触发身份证18位模式 \d{17}[0-9Xx]），需通过 Luhn 校验
        String content = "银行卡号6222021234567894";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertTrue(result.hasSensitiveContent);
        assertTrue(result.filteredContent.contains("[REDACTED_BANKCARD]"));
    }

    @Test
    void testFilterIpAddress() {
        String content = "服务器IP 192.168.1.100 不可用";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertTrue(result.hasSensitiveContent);
        assertTrue(result.filteredContent.contains("[REDACTED_IP]"));
    }

    @Test
    void testMultiplePiiTypes() {
        // 使用不含手机号子串的身份证号
        String content = "电话13812345678 邮箱a@b.com 身份证110101200003071234";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertTrue(result.hasSensitiveContent);
        assertTrue(result.detectedTypes.contains("PII"));
        assertTrue(result.totalFiltered >= 3);
    }

    // ===================== 第2层：违禁词过滤 =====================

    @Test
    void testFilterForbiddenWord() {
        String content = "这涉嫌诈骗行为";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertTrue(result.hasSensitiveContent);
        assertTrue(result.detectedTypes.contains("FORBIDDEN_WORD"));
        assertFalse(result.filteredContent.contains("诈骗"));
        assertTrue(result.filteredContent.contains("***"));
    }

    @Test
    void testFilterMultipleForbiddenWords() {
        String content = "禁止赌博和毒品交易";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertTrue(result.hasSensitiveContent);
        assertTrue(result.detectedTypes.contains("FORBIDDEN_WORD"));
        assertFalse(result.filteredContent.contains("赌博"));
        assertFalse(result.filteredContent.contains("毒品"));
    }

    // ===================== 第3层：敏感商业信息 =====================

    @Test
    void testFilterSensitiveBusinessInfo() {
        String content = "成本价:50元的商品";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertTrue(result.hasSensitiveContent);
        assertTrue(result.detectedTypes.contains("SENSITIVE_BUSINESS"));
        assertTrue(result.filteredContent.contains("[REDACTED_BUSINESS]"));
    }

    @Test
    void testFilterInternalPrice() {
        String content = "内部价80元 出货";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertTrue(result.hasSensitiveContent);
    }

    // ===================== 审计日志 =====================

    @Test
    void testAuditLogRecorded() {
        filterService.filter("手机号13812345678");
        List<Map<String, Object>> logs = filterService.getAuditLogs(10);
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        Map<String, Object> first = logs.get(0);
        assertEquals("PII_PHONE", first.get("filterType"));
        assertNotNull(first.get("timestamp"));
        assertNotNull(first.get("originalSnippet"));
        assertEquals("[REDACTED_PHONE]", first.get("filteredSnippet"));
    }

    @Test
    void testAuditLogLimit() {
        // 生成多条审计日志
        for (int i = 0; i < 15; i++) {
            filterService.filter("手机号1381234000" + (i % 10));
        }
        List<Map<String, Object>> logs = filterService.getAuditLogs(5);
        assertEquals(5, logs.size());
    }

    @Test
    void testAuditLogLimitZero() {
        filterService.filter("手机号13812345678");
        List<Map<String, Object>> logs = filterService.getAuditLogs(0);
        assertNotNull(logs);
        assertTrue(logs.isEmpty());
    }

    // ===================== 边界条件 =====================

    @Test
    void testFilterNullContent() {
        ContentFilterService.FilterResult result = filterService.filter(null);
        assertFalse(result.hasSensitiveContent);
        assertEquals("", result.filteredContent);
        assertEquals(0, result.totalFiltered);
        assertTrue(result.detectedTypes.isEmpty());
    }

    @Test
    void testFilterEmptyContent() {
        ContentFilterService.FilterResult result = filterService.filter("");
        assertFalse(result.hasSensitiveContent);
        assertEquals("", result.filteredContent);
    }

    @Test
    void testFilterNoSensitiveContent() {
        String content = "今天天气真好，适合外出";
        ContentFilterService.FilterResult result = filterService.filter(content);
        assertFalse(result.hasSensitiveContent);
        assertEquals(content, result.filteredContent);
        assertEquals(0, result.totalFiltered);
        assertTrue(result.detectedTypes.isEmpty());
    }

    // ===================== 入口方法 =====================

    @Test
    void testFilterUserQuery() {
        String query = "我的手机号13987654321被泄露了";
        String filtered = filterService.filterUserQuery(query);
        assertNotNull(filtered);
        assertFalse(filtered.contains("13987654321"));
        assertTrue(filtered.contains("[REDACTED_PHONE]"));
    }

    @Test
    void testFilterUserQueryNull() {
        String filtered = filterService.filterUserQuery(null);
        assertNull(filtered);
    }

    @Test
    void testFilterProductContext() {
        String context = "商品ID:1 | 苹果 | ¥10 | 成本价:5元 | 销量:100";
        String filtered = filterService.filterProductContext(context);
        assertNotNull(filtered);
        assertTrue(filtered.contains("[REDACTED_BUSINESS]"));
        assertFalse(filtered.contains("成本价:5"));
    }

    @Test
    void testFilterProductContextNull() {
        String filtered = filterService.filterProductContext(null);
        assertNull(filtered);
    }

    @Test
    void testFilterProductContextEmpty() {
        String filtered = filterService.filterProductContext("");
        assertEquals("", filtered);
    }

    // ===================== 审计日志字段完整性 =====================

    @Test
    void testAuditLogFieldIntegrity() {
        filterService.filter("诈骗电话13812345678");
        List<Map<String, Object>> logs = filterService.getAuditLogs(10);
        assertFalse(logs.isEmpty());
        // 应记录多条（PII + 违禁词）
        for (Map<String, Object> log : logs) {
            assertNotNull(log.get("timestamp"));
            assertNotNull(log.get("filterType"));
            assertNotNull(log.get("originalSnippet"));
            assertNotNull(log.get("filteredSnippet"));
            assertNotNull(log.get("position"));
        }
        // 至少包含 PII 和 FORBIDDEN_WORD 两种类型
        boolean hasPii = false, hasForbidden = false;
        for (Map<String, Object> log : logs) {
            String type = (String) log.get("filterType");
            if (type != null && type.startsWith("PII")) hasPii = true;
            if ("FORBIDDEN_WORD".equals(type)) hasForbidden = true;
        }
        assertTrue(hasPii, "应检测到PII类型");
        assertTrue(hasForbidden, "应检测到违禁词类型");
    }

    @Test
    void testAuditLogOrderIsNewestFirst() {
        filterService.filter("手机号13812345678");
        filterService.filter("邮箱first@test.com");
        List<Map<String, Object>> logs = filterService.getAuditLogs(10);
        assertFalse(logs.size() < 2);
        // 最近一条（邮箱）应在前面
        long ts1 = (Long) logs.get(0).get("timestamp");
        long ts2 = (Long) logs.get(1).get("timestamp");
        assertTrue(ts1 >= ts2, "审计日志应按时间倒序排列（最近在前）");
    }
}
