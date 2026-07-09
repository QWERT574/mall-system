package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识分块 Mapper，对应 knowledge_chunk 表。
 * 向量字段（embedding BLOB）的序列化/反序列化由 VectorStoreService 处理。
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {
    /** 查询某文档的所有分块 */
    List<KnowledgeChunk> selectByDocumentId(@Param("documentId") Long documentId);

    /** 查询所有已向量化的分块（用于全量检索） */
    List<KnowledgeChunk> selectAllWithEmbedding();

    /** 按文档ID范围查询带向量的分块 */
    List<KnowledgeChunk> selectWithEmbeddingByDocumentIds(@Param("documentIds") List<Long> documentIds);

    /** 插入分块（含向量 BLOB） */
    int insertChunkWithEmbedding(KnowledgeChunk chunk);

    /** 更新分块向量 */
    int updateEmbedding(@Param("id") Long id, @Param("embedding") float[] embedding,
                        @Param("embeddingModel") String embeddingModel, @Param("embeddingDim") Integer embeddingDim);

    /** 增加命中次数 */
    int incrementHitCount(@Param("id") Long id);

    /** 按文档ID删除分块 */
    int deleteByDocumentId(@Param("documentId") Long documentId);

    /** 统计已向量化的分块总数 */
    int countWithEmbedding();
}
