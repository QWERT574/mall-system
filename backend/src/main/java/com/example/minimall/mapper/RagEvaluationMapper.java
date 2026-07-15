package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.RagEvaluation;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 效果评估 Mapper，对应 rag_evaluation 表
 */
@Mapper
public interface RagEvaluationMapper extends BaseMapper<RagEvaluation> {
}
