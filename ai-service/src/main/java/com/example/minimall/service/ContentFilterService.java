package com.example.minimall.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多层敏感信息过滤服务
 *
 * 实现3层过滤：
 * 1. 基于正则表达式的PII检测（手机号、邮箱、身份证号、银行卡号等）
 * 2. 基于关键词黑名单的违禁词过滤（覆盖国家规定的违禁词库）
 * 3. 过滤结果审计日志
 */
@Service
public class ContentFilterService {
    private static final Logger logger = LoggerFactory.getLogger(ContentFilterService.class);

    /** 审计日志最大保留条数 */
    private static final int MAX_AUDIT_LOGS = 1000;

    /** 审计日志中内容片段的最大长度 */
    private static final int SNIPPET_MAX_LENGTH = 100;

    // PII正则模式
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[0-9Xx]");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    // 违禁词库（简化版，实际应从数据库或配置文件加载）
    private static final Set<String> FORBIDDEN_WORDS = new HashSet<>(Arrays.asList(
        "诈骗", "赌博", "毒品", "枪支", "弹药", "爆炸物", "色情", "反动",
        "传销", "非法集资", "洗钱", "黑客", "病毒", "木马"
    ));

    // 敏感商业信息模式
    private static final Pattern PRICE_INTERNAL_PATTERN = Pattern.compile("(?i)(成本价|进货价|内部价|底价)\\s*[:：]?\\s*\\d+");

    // 审计日志存储，最多保留 MAX_AUDIT_LOGS 条（最近一条在 deque 尾部）
    private final ConcurrentLinkedDeque<Map<String, Object>> auditLogs = new ConcurrentLinkedDeque<>();

    public static class FilterResult {
        public final String filteredContent;
        public final boolean hasSensitiveContent;
        public final List<String> detectedTypes;
        public final int totalFiltered;

        public FilterResult(String filteredContent, boolean hasSensitiveContent,
                           List<String> detectedTypes, int totalFiltered) {
            this.filteredContent = filteredContent;
            this.hasSensitiveContent = hasSensitiveContent;
            this.detectedTypes = detectedTypes;
            this.totalFiltered = totalFiltered;
        }
    }

    /** 单层过滤结果载体 */
    private static class LayerResult {
        final String content;
        final int count;
        LayerResult(String content, int count) {
            this.content = content;
            this.count = count;
        }
    }

    /**
     * 对输入内容进行多层过滤
     *
     * @param content 原始内容
     * @return 过滤结果
     */
    public FilterResult filter(String content) {
        List<String> detectedTypes = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return new FilterResult("", false, detectedTypes, 0);
        }

        String working = content;
        int totalFiltered = 0;

        // 第1层：PII检测（手机号/邮箱/身份证/银行卡/IP）
        LayerResult pii = filterPii(working);
        if (pii.count > 0) {
            detectedTypes.add("PII");
            totalFiltered += pii.count;
        }
        working = pii.content;

        // 第2层：违禁词检测
        LayerResult forbidden = filterForbiddenWords(working);
        if (forbidden.count > 0) {
            detectedTypes.add("FORBIDDEN_WORD");
            totalFiltered += forbidden.count;
        }
        working = forbidden.content;

        // 第3层：敏感商业信息检测
        LayerResult business = filterSensitiveBusiness(working);
        if (business.count > 0) {
            detectedTypes.add("SENSITIVE_BUSINESS");
            totalFiltered += business.count;
        }
        working = business.content;

        boolean hasSensitive = !detectedTypes.isEmpty();
        if (hasSensitive) {
            logger.warn("Sensitive content filtered: types={}, totalFiltered={}", detectedTypes, totalFiltered);
        }

        return new FilterResult(working, hasSensitive, detectedTypes, totalFiltered);
    }

    /**
     * 过滤商品上下文（用于注入LLM prompt前）
     *
     * @param productContext 商品上下文
     * @return 过滤后的内容
     */
    public String filterProductContext(String productContext) {
        if (productContext == null || productContext.isEmpty()) {
            return productContext;
        }
        FilterResult result = filter(productContext);
        if (result.hasSensitiveContent) {
            logger.info("Product context filtered: types={}, count={}",
                    result.detectedTypes, result.totalFiltered);
        }
        return result.filteredContent;
    }

    /**
     * 过滤用户查询
     *
     * @param query 用户查询
     * @return 过滤后的内容
     */
    public String filterUserQuery(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }
        FilterResult result = filter(query);
        if (result.hasSensitiveContent) {
            logger.info("User query filtered: types={}, count={}",
                    result.detectedTypes, result.totalFiltered);
        }
        return result.filteredContent;
    }

    /**
     * 获取审计日志（最近N条，按时间倒序：最近一条在前）
     *
     * @param limit 返回条数
     * @return 审计日志列表
     */
    public List<Map<String, Object>> getAuditLogs(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (limit <= 0) {
            return result;
        }
        synchronized (auditLogs) {
            Iterator<Map<String, Object>> desc = auditLogs.descendingIterator();
            int n = 0;
            while (desc.hasNext() && n < limit) {
                result.add(desc.next());
                n++;
            }
        }
        return result;
    }

    // ===================== 内部实现 =====================

    /**
     * 第1层：PII检测与替换
     * 顺序很重要：身份证（18位）需在银行卡（16-19位）之前处理，避免误匹配
     */
    private LayerResult filterPii(String content) {
        int count = 0;
        LayerResult r;

        r = applyPattern(content, PHONE_PATTERN, "[REDACTED_PHONE]", "PII_PHONE");
        count += r.count;
        content = r.content;

        r = applyPattern(content, EMAIL_PATTERN, "[REDACTED_EMAIL]", "PII_EMAIL");
        count += r.count;
        content = r.content;

        r = applyPattern(content, ID_CARD_PATTERN, "[REDACTED_IDCARD]", "PII_ID_CARD");
        count += r.count;
        content = r.content;

        r = filterBankCard(content);
        count += r.count;
        content = r.content;

        r = applyPattern(content, IP_PATTERN, "[REDACTED_IP]", "PII_IP");
        count += r.count;
        content = r.content;

        return new LayerResult(content, count);
    }

    /**
     * 第2层：违禁词检测与替换
     */
    private LayerResult filterForbiddenWords(String content) {
        int totalCount = 0;
        String working = content;
        for (String word : FORBIDDEN_WORDS) {
            LayerResult r = applyLiteral(working, word, "***", "FORBIDDEN_WORD");
            totalCount += r.count;
            working = r.content;
        }
        return new LayerResult(working, totalCount);
    }

    /**
     * 第3层：敏感商业信息检测与替换
     */
    private LayerResult filterSensitiveBusiness(String content) {
        return applyPattern(content, PRICE_INTERNAL_PATTERN, "[REDACTED_BUSINESS]", "SENSITIVE_BUSINESS");
    }

    /**
     * 银行卡检测：正则候选 + Luhn 校验，避免误匹配订单号/物流单号
     */
    private LayerResult filterBankCard(String content) {
        Matcher matcher = BANK_CARD_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        int count = 0;
        while (matcher.find()) {
            String matched = matcher.group();
            // 仅对通过 Luhn 校验的数字串执行替换，过滤掉订单号/物流单号等非银行卡数字
            if (luhnCheck(matched)) {
                count++;
                int position = matcher.start();
                recordAuditLog("PII_BANK_CARD", matched, "[REDACTED_BANKCARD]", position);
                matcher.appendReplacement(result, Matcher.quoteReplacement("[REDACTED_BANKCARD]"));
            }
        }
        matcher.appendTail(result);
        return new LayerResult(result.toString(), count);
    }

    /**
     * Luhn 校验算法：银行卡号标准合法性校验
     * 奇数位（从右起）×2 超过 9 则减 9，所有位数之和能被 10 整除即为合法银行卡号
     */
    private boolean luhnCheck(String number) {
        if (number == null || number.length() < 16 || number.length() > 19) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int d = number.charAt(i) - '0';
            if (d < 0 || d > 9) return false;
            if (alternate) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /**
     * 使用正则模式匹配并替换，同时记录审计日志
     * 使用 StringBuffer 以兼容 Java 8（appendReplacement 的 StringBuilder 重载在 Java 9 才引入）
     */
    private LayerResult applyPattern(String content, Pattern pattern, String replacement, String filterType) {
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();
        int count = 0;
        while (matcher.find()) {
            count++;
            String matched = matcher.group();
            int position = matcher.start();
            recordAuditLog(filterType, matched, replacement, position);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return new LayerResult(result.toString(), count);
    }

    /**
     * 字面量匹配并替换（用于违禁词，避免正则元字符干扰）
     */
    private LayerResult applyLiteral(String content, String literal, String replacement, String filterType) {
        if (content == null || content.isEmpty() || literal == null || literal.isEmpty()) {
            return new LayerResult(content, 0);
        }
        if (!content.contains(literal)) {
            return new LayerResult(content, 0);
        }
        return applyPattern(content, Pattern.compile(Pattern.quote(literal)), replacement, filterType);
    }

    /**
     * 记录一条审计日志，超出上限时淘汰最旧的一条
     */
    private void recordAuditLog(String filterType, String originalSnippet, String filteredSnippet, int position) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", System.currentTimeMillis());
        entry.put("filterType", filterType);
        entry.put("originalSnippet", truncate(originalSnippet, SNIPPET_MAX_LENGTH));
        entry.put("filteredSnippet", truncate(filteredSnippet, SNIPPET_MAX_LENGTH));
        entry.put("position", position);

        synchronized (auditLogs) {
            auditLogs.addLast(entry);
            while (auditLogs.size() > MAX_AUDIT_LOGS) {
                auditLogs.pollFirst();
            }
        }
    }

    /**
     * 截断字符串到指定长度，防止审计日志存储过长内容
     */
    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
