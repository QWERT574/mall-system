package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识文档 Mapper，对应 knowledge_document 表
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {
    /** 按状态查询文档 */
    List<KnowledgeDocument> selectByStatus(@Param("status") Integer status);

    /** 按分类查询文档 */
    List<KnowledgeDocument> selectByCategory(@Param("category") String category);

    /** 更新文档的分块数量和状态 */
    int updateChunkCountAndStatus(@Param("id") Long id, @Param("chunkCount") int chunkCount, @Param("status") int status);

    /** 按标题模糊搜索 */
    List<KnowledgeDocument> searchByTitle(@Param("keyword") String keyword);
}
