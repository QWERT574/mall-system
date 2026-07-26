package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对话消息 Mapper，对应 conversation_message 表
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
    /** 按会话ID查询最近N条消息（按时间正序） */
    List<ConversationMessage> selectRecentBySessionId(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    /** 按会话ID查询所有消息 */
    List<ConversationMessage> selectBySessionId(@Param("sessionId") Long sessionId);
}
