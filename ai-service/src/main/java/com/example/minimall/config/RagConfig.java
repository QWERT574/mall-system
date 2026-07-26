package com.example.minimall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 检索增强生成管道配置。
 * <p>
 * 控制文档分块策略、检索 top-k、相似度阈值、多轮对话上下文窗口等核心参数。
 * 所有参数均可通过 application.yml 中的 rag.* 前缀覆盖，也支持环境变量注入。
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {
    /** 是否启用 RAG（关闭则走原有直接 LLM 调用逻辑） */
    private boolean enabled = true;
    /** 文档分块大小（字符数） */
    private int chunkSize = 500;
    /** 分块重叠大小（字符数） */
    private int chunkOverlap = 100;
    /** 检索 top-k 数量 */
    private int topK = 5;
    /** FAQ 检索 top-k */
    private int faqTopK = 3;
    /** 余弦相似度阈值 */
    private double similarityThreshold = 0.65;
    /** 对话上下文保留轮数 */
    private int conversationHistoryTurns = 6;
    /** 上下文最大 token 数 */
    private int maxContextTokens = 3000;
    /** 是否启用多轮对话 */
    private boolean multiTurnEnabled = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public int getFaqTopK() { return faqTopK; }
    public void setFaqTopK(int faqTopK) { this.faqTopK = faqTopK; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public int getConversationHistoryTurns() { return conversationHistoryTurns; }
    public void setConversationHistoryTurns(int conversationHistoryTurns) { this.conversationHistoryTurns = conversationHistoryTurns; }
    public int getMaxContextTokens() { return maxContextTokens; }
    public void setMaxContextTokens(int maxContextTokens) { this.maxContextTokens = maxContextTokens; }
    public boolean isMultiTurnEnabled() { return multiTurnEnabled; }
    public void setMultiTurnEnabled(boolean multiTurnEnabled) { this.multiTurnEnabled = multiTurnEnabled; }
}
