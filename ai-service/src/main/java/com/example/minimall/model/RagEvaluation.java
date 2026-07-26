package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RAG 效果评估记录实体，对应 rag_evaluation 表。
 * 用于存储同一问题在 RAG 增强 vs 基线（无 RAG）下的回答对比数据，支撑效果分析。
 */
@TableName("rag_evaluation")
public class RagEvaluation {
    @TableId
    private Long id;
    private String query;
    @TableField("rag_answer")
    private String ragAnswer;
    @TableField("baseline_answer")
    private String baselineAnswer;
    @TableField("rag_response_time_ms")
    private Integer ragResponseTimeMs;
    @TableField("baseline_response_time_ms")
    private Integer baselineResponseTimeMs;
    @TableField("rag_retrieval_time_ms")
    private Integer ragRetrievalTimeMs;
    @TableField("rag_source_count")
    private Integer ragSourceCount;
    @TableField("accuracy_score")
    private BigDecimal accuracyScore;
    @TableField("relevance_score")
    private BigDecimal relevanceScore;
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getRagAnswer() { return ragAnswer; }
    public void setRagAnswer(String ragAnswer) { this.ragAnswer = ragAnswer; }
    public String getBaselineAnswer() { return baselineAnswer; }
    public void setBaselineAnswer(String baselineAnswer) { this.baselineAnswer = baselineAnswer; }
    public Integer getRagResponseTimeMs() { return ragResponseTimeMs; }
    public void setRagResponseTimeMs(Integer ragResponseTimeMs) { this.ragResponseTimeMs = ragResponseTimeMs; }
    public Integer getBaselineResponseTimeMs() { return baselineResponseTimeMs; }
    public void setBaselineResponseTimeMs(Integer baselineResponseTimeMs) { this.baselineResponseTimeMs = baselineResponseTimeMs; }
    public Integer getRagRetrievalTimeMs() { return ragRetrievalTimeMs; }
    public void setRagRetrievalTimeMs(Integer ragRetrievalTimeMs) { this.ragRetrievalTimeMs = ragRetrievalTimeMs; }
    public Integer getRagSourceCount() { return ragSourceCount; }
    public void setRagSourceCount(Integer ragSourceCount) { this.ragSourceCount = ragSourceCount; }
    public BigDecimal getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(BigDecimal accuracyScore) { this.accuracyScore = accuracyScore; }
    public BigDecimal getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(BigDecimal relevanceScore) { this.relevanceScore = relevanceScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
