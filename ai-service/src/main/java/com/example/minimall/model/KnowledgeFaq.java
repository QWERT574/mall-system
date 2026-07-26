package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * FAQ 结构化问答对实体，对应 knowledge_faq 表。
 * 高频问题以 Q&A 形式直接存储，检索时优先匹配，命中后直接返回标准答案。
 */
@TableName("knowledge_faq")
public class KnowledgeFaq {
    @TableId
    private Long id;
    private String question;
    private String answer;
    @TableField(exist = false)
    private float[] questionEmbedding;
    private String category;
    private String keywords;
    private Integer priority;
    @TableField("hit_count")
    private Integer hitCount;
    private Integer status;
    @TableField("embedding_model")
    private String embeddingModel;
    @TableField("created_by")
    private Long createdBy;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public float[] getQuestionEmbedding() { return questionEmbedding; }
    public void setQuestionEmbedding(float[] questionEmbedding) { this.questionEmbedding = questionEmbedding; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getHitCount() { return hitCount; }
    public void setHitCount(Integer hitCount) { this.hitCount = hitCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
