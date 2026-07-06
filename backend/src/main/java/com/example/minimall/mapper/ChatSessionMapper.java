package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天会话表 Mapper，对应 chat_session 表
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /** 查询会话详情（用户、商家、客服、商品及最后一条消息） */
    @Select("SELECT cs.*, " +
            "COALESCE(u.nickname, u.username, CONCAT('用户', u.id)) as userName, " +
            "COALESCE(u2.nickname, u2.username, CONCAT('商家', u2.id)) as sellerName, " +
            "p.name as productName, " +
            "COALESCE(a.nickname, a.username) as agentName, " +
            "(SELECT cm.content FROM chat_message cm WHERE cm.session_id = cs.id ORDER BY cm.created_at DESC LIMIT 1) as lastMessage " +
            "FROM chat_session cs " +
            "LEFT JOIN user u ON cs.user_id = u.id " +
            "LEFT JOIN user u2 ON cs.seller_id = u2.id " +
            "LEFT JOIN product p ON cs.product_id = p.id " +
            "LEFT JOIN user a ON cs.agent_id = a.id " +
            "WHERE cs.id = #{sessionId}")
    ChatSession selectSessionDetail(@Param("sessionId") Long sessionId);

    /** 查询指定用户的会话列表（按最后消息时间倒序） */
    @Select("SELECT cs.*, " +
            "COALESCE(u.nickname, u.username, CONCAT('用户', u.id)) as userName, " +
            "COALESCE(u2.nickname, u2.username, CONCAT('商家', u2.id)) as sellerName, " +
            "p.name as productName, " +
            "COALESCE(a.nickname, a.username) as agentName, " +
            "(SELECT cm.content FROM chat_message cm WHERE cm.session_id = cs.id ORDER BY cm.created_at DESC LIMIT 1) as lastMessage " +
            "FROM chat_session cs " +
            "LEFT JOIN user u ON cs.user_id = u.id " +
            "LEFT JOIN user u2 ON cs.seller_id = u2.id " +
            "LEFT JOIN product p ON cs.product_id = p.id " +
            "LEFT JOIN user a ON cs.agent_id = a.id " +
            "WHERE cs.user_id = #{userId} " +
            "ORDER BY cs.last_message_at DESC")
    List<ChatSession> selectByUserId(@Param("userId") Long userId);

    /** 查询指定商家的会话列表 */
    @Select("SELECT cs.*, " +
            "COALESCE(u.nickname, u.username, CONCAT('用户', u.id)) as userName, " +
            "COALESCE(u2.nickname, u2.username, CONCAT('商家', u2.id)) as sellerName, " +
            "p.name as productName, " +
            "COALESCE(a.nickname, a.username) as agentName, " +
            "(SELECT cm.content FROM chat_message cm WHERE cm.session_id = cs.id ORDER BY cm.created_at DESC LIMIT 1) as lastMessage " +
            "FROM chat_session cs " +
            "LEFT JOIN user u ON cs.user_id = u.id " +
            "LEFT JOIN user u2 ON cs.seller_id = u2.id " +
            "LEFT JOIN product p ON cs.product_id = p.id " +
            "LEFT JOIN user a ON cs.agent_id = a.id " +
            "WHERE cs.seller_id = #{sellerId} " +
            "ORDER BY cs.last_message_at DESC")
    List<ChatSession> selectBySellerId(@Param("sellerId") Long sellerId);

    /** 查询指定客服坐席的会话列表 */
    @Select("SELECT cs.*, " +
            "COALESCE(u.nickname, u.username, CONCAT('用户', u.id)) as userName, " +
            "COALESCE(u2.nickname, u2.username, CONCAT('商家', u2.id)) as sellerName, " +
            "p.name as productName, " +
            "COALESCE(a.nickname, a.username) as agentName, " +
            "(SELECT cm.content FROM chat_message cm WHERE cm.session_id = cs.id ORDER BY cm.created_at DESC LIMIT 1) as lastMessage " +
            "FROM chat_session cs " +
            "LEFT JOIN user u ON cs.user_id = u.id " +
            "LEFT JOIN user u2 ON cs.seller_id = u2.id " +
            "LEFT JOIN product p ON cs.product_id = p.id " +
            "LEFT JOIN user a ON cs.agent_id = a.id " +
            "WHERE cs.agent_id = #{agentId} " +
            "ORDER BY cs.last_message_at DESC")
    List<ChatSession> selectByAgentId(@Param("agentId") Long agentId);
}
