package com.example.minimall.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/** 敏感词过滤服务，检测并替换聊天内容中的敏感词 */
@Service
public class SensitiveWordFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveWordFilter.class);

    /** 敏感词集合 */
    private final Set<String> sensitiveWords = new HashSet<>();

    public SensitiveWordFilter() {
        initSensitiveWords();
    }

    private void initSensitiveWords() {
        sensitiveWords.add("fuck");
        sensitiveWords.add("shit");
        sensitiveWords.add("asshole");
        sensitiveWords.add("bitch");
        sensitiveWords.add("dick");
        sensitiveWords.add("bastard");
        sensitiveWords.add("damn");

        sensitiveWords.add("傻逼");
        sensitiveWords.add("操你");
        sensitiveWords.add("妈的");
        sensitiveWords.add("sb");
        sensitiveWords.add("cnm");
        sensitiveWords.add("nmsl");
        sensitiveWords.add("草泥马");
        sensitiveWords.add("碧池");
        sensitiveWords.add("去死");
    }

    /**
     * 判断文本是否包含敏感词
     * <p>大小写不敏感；命中任一敏感词即返回 true，并打 warn 日志（不记录原文）</p>
     *
     * @param text 待检测文本
     * @return 是否包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();

        for (String word : sensitiveWords) {
            if (lowerText.contains(word.toLowerCase())) {
                logger.warn("Sensitive word detected in text (length={})", text.length());
                return true;
            }
        }

        return false;
    }

    /** 替换文本中的敏感词为星号 */
    public String filter(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String result = text;
        for (String word : sensitiveWords) {
            String replacement = buildReplacement(word.length());
            result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(word), replacement);
        }

        return result;
    }

    /**
     * 生成等长的星号替换串
     *
     * @param length 长度
     * @return 由 length 个 '*' 组成的字符串
     */
    private String buildReplacement(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append('*');
        }
        return sb.toString();
    }
}
