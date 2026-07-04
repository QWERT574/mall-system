package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息表 Mapper，对应 chat_message 表
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /** 查询会话下的所有聊天消息（按时间正序） */
    @Select("SELECT cm.*, u.nickname as senderName FROM chat_message cm " +
            "LEFT JOIN user u ON cm.sender_id = u.id " +
            "WHERE cm.session_id = #{sessionId} " +
            "ORDER BY cm.created_at ASC")
    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId);

    /** 分页查询指定会话的聊天消息 */
    @Select("SELECT cm.*, u.nickname as senderName FROM chat_message cm " +
            "LEFT JOIN user u ON cm.sender_id = u.id " +
            "WHERE cm.session_id = #{sessionId} " +
            "ORDER BY cm.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<ChatMessage> selectBySessionIdPaged(@Param("sessionId") Long sessionId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    /** 查询指定消息 ID 之后的新消息 */
    @Select("SELECT cm.*, u.nickname as senderName FROM chat_message cm " +
            "LEFT JOIN user u ON cm.sender_id = u.id " +
            "WHERE cm.session_id = #{sessionId} AND cm.id > #{afterId} ORDER BY cm.id ASC")
    List<ChatMessage> selectBySessionIdAfterId(@Param("sessionId") Long sessionId,
                                                @Param("afterId") Long afterId);

    /** 查询指定用户的未读消息（来自客服） */
    @Select("SELECT cm.*, u.nickname as senderName FROM chat_message cm " +
            "LEFT JOIN user u ON cm.sender_id = u.id " +
            "WHERE cm.receiver_id = #{userId} AND cm.sender_type = 2 AND cm.is_read = 0")
    List<ChatMessage> selectUnreadMessages(@Param("userId") Long userId);
}
