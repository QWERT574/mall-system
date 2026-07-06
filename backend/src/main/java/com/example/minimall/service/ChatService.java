package com.example.minimall.service;

import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatSession;

import java.util.List;

/** 聊天会话与消息业务接口 */
public interface ChatService {
    /**
     * 获取或创建聊天会话
     * <p>优先复用 (userId, sellerId, productId) 维度的已有会话</p>
     */
    ChatSession getOrCreateSession(Long userId, Long sellerId, Long productId, Long orderId);
    /** 发送文本消息 */
    ChatMessage sendTextMessage(Long sessionId, Long senderId, Integer senderType,
                                Long receiverId, String content, Long relatedOrderId, Long relatedProductId);
    /** 发送图片消息 */
    ChatMessage sendImageMessage(Long sessionId, Long senderId, Integer senderType,
                                 Long receiverId, String imageUrl, String content,
                                 Long relatedOrderId, Long relatedProductId);
    /** 发送文件消息 */
    ChatMessage sendFileMessage(Long sessionId, Long senderId, Integer senderType,
                                Long receiverId, String content, String fileName,
                                Long fileSize, String fileUrl);
    /** 更新消息 */
    void updateMessage(ChatMessage message);
    /** 插入消息（**不入库会话**） */
    void insertMessage(ChatMessage message);
    /**
     * 更新会话最后一条消息时间
     * <p>通常在 sendXxxMessage 内部调用</p>
     */
    void updateSessionLastMessageTimestamp(Long sessionId);
    /** 获取会话消息列表 */
    List<ChatMessage> getMessages(Long sessionId);
    /** 分页获取会话消息（**按时间倒序**） */
    List<ChatMessage> getMessagesPaged(Long sessionId, int page, int size);
    /**
     * 获取指定消息 ID 之后的新消息（**用于轮询/增量同步**）
     */
    List<ChatMessage> getMessagesAfterId(Long sessionId, Long afterId);
    /** 获取用户的会话列表 */
    List<ChatSession> getUserSessions(Long userId);
    /** 获取商家的会话列表 */
    List<ChatSession> getSellerSessions(Long sellerId);
    /** 获取客服的会话列表 */
    List<ChatSession> getAgentSessions(Long agentId);
    /**
     * 标记会话消息为已读
     * <p>同时清空 Redis 中对应未读计数</p>
     */
    void markMessagesAsRead(Long sessionId, Long userId, Integer userType);
    /** 获取会话详情（**含消息统计、最后消息等**） */
    ChatSession getSessionDetail(Long sessionId);
    /** 根据用户和商家查找会话 ID（**找不到返回 null**） */
    Long findSessionIdByUserIdAndSellerId(Long userId, Long sellerId);
    /**
     * 强制创建新会话
     * <p>与 getOrCreateSession 不同，**总会新建一条记录**</p>
     */
    Long createSession(Long userId, Long sellerId, Long productId, Long orderId);
    /**
     * 关闭会话
     * <p>关闭后双方只读，由 closeReason 记录关闭原因</p>
     */
    void closeSession(Long sessionId, String closeReason);
    /**
     * 分配客服
     * <p>把指定会话分配给某客服，被分配的客服将出现在 agentSessions 中</p>
     */
    void assignAgent(Long sessionId, Long agentId);
}
