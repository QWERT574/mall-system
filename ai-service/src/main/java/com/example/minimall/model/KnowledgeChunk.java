package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识分块实体，对应 knowledge_chunk 表。
 * 存储文档分块文本及其向量嵌入（embedding 字段为 float[] 序列化的 BLOB）。
 */
@TableName("knowledge_chunk")
public class KnowledgeChunk {
    @TableId
    private Long id;
    @TableField("document_id")
    private Long documentId;
    @TableField("chunk_index")
    private Integer chunkIndex;
    private String content;
    /** 向量嵌入（float[] 序列化），不直接映射为 Java 字段，由 VectorStoreService 管理 */
    @TableField(exist = false)
    private float[] embedding;
    @TableField("embedding_model")
    private String embeddingModel;
    @TableField("embedding_dim")
    private Integer embeddingDim;
    @TableField("token_count")
    private Integer tokenCount;
    @TableField("chunk_meta")
    private String chunkMeta;
    @TableField("score_avg")
    private BigDecimal scoreAvg;
    @TableField("hit_count")
    private Integer hitCount;
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public Integer getEmbeddingDim() { return embeddingDim; }
    public void setEmbeddingDim(Integer embeddingDim) { this.embeddingDim = embeddingDim; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public String getChunkMeta() { return chunkMeta; }
    public void setChunkMeta(String chunkMeta) { this.chunkMeta = chunkMeta; }
    public BigDecimal getScoreAvg() { return scoreAvg; }
    public void setScoreAvg(BigDecimal scoreAvg) { this.scoreAvg = scoreAvg; }
    public Integer getHitCount() { return hitCount; }
    public void setHitCount(Integer hitCount) { this.hitCount = hitCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
