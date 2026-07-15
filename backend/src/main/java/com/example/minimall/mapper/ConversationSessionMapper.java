package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ConversationSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对话会话 Mapper，对应 conversation_session 表
 */
@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSession> {
    /** 按 sessionToken 查询 */
    ConversationSession selectByToken(@Param("sessionToken") String sessionToken);

    /** 按用户ID查询活跃会话 */
    List<ConversationSession> selectActiveByUserId(@Param("userId") Long userId);

    /** 更新会话上下文摘要 */
    int updateContextSummary(@Param("id") Long id, @Param("contextSummary") String contextSummary,
                             @Param("messageCount") Integer messageCount);

    /** 关闭会话 */
    int closeSession(@Param("id") Long id);
}
