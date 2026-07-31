package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 对话消息实体，对应 conversation_message 表。
 * 记录每轮对话的完整信息，包括检索到的知识分块、FAQ、来源溯源等可解释性数据。
 */
@TableName("conversation_message")
public class ConversationMessage {
    @TableId
    private Long id;
    @TableField("session_id")
    private Long sessionId;
    private String role;
    private String content;
    @TableField("retrieved_chunks")
    private String retrievedChunks;
    @TableField("retrieved_faqs")
    private String retrievedFaqs;
    private String sources;
    @TableField("retrieval_score")
    private BigDecimal retrievalScore;
    @TableField("response_time_ms")
    private Integer responseTimeMs;
    @TableField("token_count")
    private Integer tokenCount;
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getRetrievedChunks() { return retrievedChunks; }
    public void setRetrievedChunks(String retrievedChunks) { this.retrievedChunks = retrievedChunks; }
    public String getRetrievedFaqs() { return retrievedFaqs; }
    public void setRetrievedFaqs(String retrievedFaqs) { this.retrievedFaqs = retrievedFaqs; }
    public String getSources() { return sources; }
    public void setSources(String sources) { this.sources = sources; }
    public BigDecimal getRetrievalScore() { return retrievalScore; }
    public void setRetrievalScore(BigDecimal retrievalScore) { this.retrievalScore = retrievalScore; }
    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
