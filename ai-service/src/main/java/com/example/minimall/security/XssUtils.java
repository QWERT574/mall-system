package com.example.minimall.security;

import java.util.regex.Pattern;

/**
 * XSS 防护工具类。
 * <p>
 * 提供 HTML/脚本特征清洗、HTML 实体转义以及搜索关键词合法性校验，
 * 与 {@code XssRequestWrapper} / {@code XssFilter} 配合完成输入侧 XSS 防护。
 * </p>
 */
public class XssUtils {
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IMG_ONERROR_PATTERN = Pattern.compile("on\\w+\\s*=\\s*['\"]?[^'\"\\s>]*['\"]?", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVAL_PATTERN = Pattern.compile("eval\\s*\\([^)]*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("expression\\s*\\([^)]*\\)", Pattern.CASE_INSENSITIVE);
    
    private static final String[] HTML_ESCAPE_MAP = {
        "<", "&lt;",
        ">", "&gt;",
        "\"", "&quot;",
        "'", "&#x27;",
        "/", "&#x2F;",
        "=", "&#x3D;"
    };
    
    /**
     * 清洗输入字符串：移除常见脚本片段并做 HTML 实体转义。
     *
     * @param input 原始字符串（可为 null）
     * @return 清洗后的安全字符串
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String sanitized = input;
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = IMG_ONERROR_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = EVAL_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = EXPRESSION_PATTERN.matcher(sanitized).replaceAll("");
        
        sanitized = escapeHtml(sanitized);
        
        return sanitized.trim();
    }
    
    /**
     * 将 HTML 特殊字符（&lt;、&gt;、&amp;、&quot;、&#x27; 等）转义为实体，避免浏览器解析。
     */
    public static String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String result = input;
        for (int i = 0; i < HTML_ESCAPE_MAP.length; i += 2) {
            result = result.replace(HTML_ESCAPE_MAP[i], HTML_ESCAPE_MAP[i + 1]);
        }
        return result;
    }
    
    /**
     * 校验搜索关键词是否合法：非空、长度不超过 100、未包含 script/javascript 等特征。
     */
    public static boolean isValidSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        if (keyword.length() > 100) {
            return false;
        }
        return !SCRIPT_PATTERN.matcher(keyword).find() && 
               !JAVASCRIPT_PATTERN.matcher(keyword).find();
    }
    
    /**
     * 私有构造方法，禁止实例化（工具类）。
     */
    private XssUtils() {
    }
}
