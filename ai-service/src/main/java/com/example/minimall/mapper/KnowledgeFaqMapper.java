package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.KnowledgeFaq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * FAQ 问答对 Mapper，对应 knowledge_faq 表
 */
@Mapper
public interface KnowledgeFaqMapper extends BaseMapper<KnowledgeFaq> {
    /** 查询所有启用的FAQ */
    List<KnowledgeFaq> selectAllEnabled();

    /** 按分类查询 */
    List<KnowledgeFaq> selectByCategory(@Param("category") String category);

    /** 更新FAQ向量 */
    int updateEmbedding(@Param("id") Long id, @Param("embedding") float[] embedding,
                        @Param("embeddingModel") String embeddingModel);

    /** 增加命中次数 */
    int incrementHitCount(@Param("id") Long id);

    /** 按关键词搜索 */
    List<KnowledgeFaq> searchByKeyword(@Param("keyword") String keyword);
}
