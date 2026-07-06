package com.example.minimall.service.impl;

import com.example.minimall.mapper.ChatMessageMapper;
import com.example.minimall.mapper.ChatSessionMapper;
import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatSession;
import com.example.minimall.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 聊天会话与消息业务实现 */
@Service
public class ChatServiceImpl implements ChatService {

    /** 聊天消息Mapper */
    @Autowired
    private ChatMessageMapper chatMessageMapper;

    /** 聊天会话Mapper */
    @Autowired
    private ChatSessionMapper chatSessionMapper;

    /**
     * 获取或创建用户-商家会话
     * <p>
     * 优先复用已存在的（userId, sellerId, productId）组合会话；找不到则创建。
     * </p>
     *
     * @param userId    用户 ID
     * @param sellerId  商家 ID
     * @param productId 关联商品 ID（可空）
     * @param orderId   关联订单 ID（可空）
     * @return 会话详情（含消息统计等关联数据）
     */
    @Transactional
    public ChatSession getOrCreateSession(Long userId, Long sellerId, Long productId, Long orderId) {
        // 1. 查找该用户的所有会话，匹配相同卖家+商品则复用
        List<ChatSession> sessions = chatSessionMapper.selectByUserId(userId);
        for (ChatSession session : sessions) {
            if (session.getSellerId().equals(sellerId) &&
                (productId == null || session.getProductId() != null && session.getProductId().equals(productId))) {
                return session;
            }
        }

        // 2. 未找到 → 创建新会话（默认值见构造）
        ChatSession newSession = new ChatSession();
        newSession.setUserId(userId);
        newSession.setSellerId(sellerId);
        newSession.setProductId(productId);
        newSession.setOrderId(orderId);
        newSession.setStatus(0);           // 0-待接入
        newSession.setSessionType(1);      // 1-用户-商家
        newSession.setAutoReplyEnabled(1); // 默认开启自动回复
        newSession.setUserUnread(0);
        newSession.setSellerUnread(0);
        newSession.setLastMessageAt(LocalDateTime.now());
        newSession.setCreatedAt(LocalDateTime.now());
        newSession.setUpdatedAt(LocalDateTime.now());

        chatSessionMapper.insert(newSession);
        return chatSessionMapper.selectSessionDetail(newSession.getId());
    }

    /**
     * 发送文本消息
     *
     * @param sessionId      会话 ID
     * @param senderId       发送者 ID（用户/商家/客服）
     * @param senderType     发送者类型：1-用户 2-商家 3-客服
     * @param receiverId     接收者 ID
     * @param content        文本内容
     * @param relatedOrderId 关联订单 ID（可空）
     * @param relatedProductId 关联商品 ID（可空）
     * @return 已落库的消息实体
     */
    @Transactional
    public ChatMessage sendTextMessage(Long sessionId, Long senderId, Integer senderType,
                                       Long receiverId, String content, Long relatedOrderId, Long relatedProductId) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderId(senderId);
        message.setSenderType(senderType);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(1);
        message.setRelatedOrderId(relatedOrderId);
        message.setRelatedProductId(relatedProductId);
        message.setIsRead(0);
        message.setStatus(1);
        message.setIsAutoReply(0);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());

        chatMessageMapper.insert(message);
        updateSessionLastMessage(sessionId, senderType);

        return message;
    }

    /**
     * 发送图片消息（**事务**）
     *
     * @param sessionId      会话 ID
     * @param senderId       发送者 ID
     * @param senderType     发送者类型：1-用户 2-商家 3-客服
     * @param receiverId     接收者 ID
     * @param imageUrl       图片 URL
     * @param content        附加文字（可空）
     * @param relatedOrderId 关联订单 ID（可空）
     * @param relatedProductId 关联商品 ID（可空）
     * @return 已落库的消息实体
     */
    @Transactional
    public ChatMessage sendImageMessage(Long sessionId, Long senderId, Integer senderType,
                                        Long receiverId, String imageUrl, String content,
                                        Long relatedOrderId, Long relatedProductId) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderId(senderId);
        message.setSenderType(senderType);
        message.setReceiverId(receiverId);
        message.setContent(content != null ? content : "[图片]");
        message.setImageUrl(imageUrl);
        message.setMessageType(2);
        message.setRelatedOrderId(relatedOrderId);
        message.setRelatedProductId(relatedProductId);
        message.setIsRead(0);
        message.setStatus(1);
        message.setIsAutoReply(0);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());

        chatMessageMapper.insert(message);
        updateSessionLastMessage(sessionId, senderType);

        return message;
    }

    /**
     * 发送文件消息
     *
     * @param sessionId  会话 ID
     * @param senderId   发送者 ID
     * @param senderType 发送者类型
     * @param receiverId 接收者 ID
     * @param content    附加文字（可空）
     * @param fileName   文件名
     * @param fileSize   文件大小（字节）
     * @param fileUrl    文件 URL
     * @return 已落库的消息实体
     */
    @Transactional
    public ChatMessage sendFileMessage(Long sessionId, Long senderId, Integer senderType,
                                       Long receiverId, String content, String fileName,
                                       Long fileSize, String fileUrl) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderId(senderId);
        message.setSenderType(senderType);
        message.setReceiverId(receiverId);
        message.setContent(content != null ? content : "[文件] " + fileName);
        message.setImageUrl(fileUrl);
        message.setFileName(fileName);
        message.setFileSize(fileSize);
        message.setMessageType(3);
        message.setIsRead(0);
        message.setStatus(1);
        message.setIsAutoReply(0);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());

        chatMessageMapper.insert(message);
        updateSessionLastMessage(sessionId, senderType);

        return message;
    }

    @Transactional
    /** 更新消息内容 */
    public void updateMessage(ChatMessage message) {
        chatMessageMapper.updateById(message);
    }

    private void updateSessionLastMessage(Long sessionId, Integer senderType) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setLastMessageAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            if (senderType == 1) {
                session.setSellerUnread(
                    (session.getSellerUnread() != null ? session.getSellerUnread() : 0) + 1);
            } else {
                session.setUserUnread(
                    (session.getUserUnread() != null ? session.getUserUnread() : 0) + 1);
            }
            chatSessionMapper.updateById(session);
        }
    }

    /** 插入一条消息 */
    @Transactional
    public void insertMessage(ChatMessage message) {
        chatMessageMapper.insert(message);
    }

    /** 更新会话最后一条消息的时间戳 */
    @Transactional
    public void updateSessionLastMessageTimestamp(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setLastMessageAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }

    /** 获取会话内的全部消息 */
    public List<ChatMessage> getMessages(Long sessionId) {
        return chatMessageMapper.selectBySessionId(sessionId);
    }

    /** 分页获取会话消息（**按时间倒序**） */
    public List<ChatMessage> getMessagesPaged(Long sessionId, int page, int size) {
        return chatMessageMapper.selectBySessionIdPaged(sessionId, (page - 1) * size, size);
    }

    /** 获取指定消息ID之后的新消息 */
    public List<ChatMessage> getMessagesAfterId(Long sessionId, Long afterId) {
        return chatMessageMapper.selectBySessionIdAfterId(sessionId, afterId);
    }

    public List<ChatSession> getUserSessions(Long userId) {
        return chatSessionMapper.selectByUserId(userId);
    }

    public List<ChatSession> getSellerSessions(Long sellerId) {
        return chatSessionMapper.selectBySellerId(sellerId);
    }

    /** 获取客服所负责的会话列表 */
    public List<ChatSession> getAgentSessions(Long agentId) {
        return chatSessionMapper.selectByAgentId(agentId);
    }

    /**
     * 标记会话内消息为已读
     * <p>
     * 按用户类型判断该读哪些消息：
     * <ul>
     *   <li>用户（1）：标记商家/客服发的消息</li>
     *   <li>商家（2）：标记用户发的消息</li>
     *   <li>客服（3）：标记用户/商家发的消息</li>
     * </ul>
     * 同时清零对应未读数。
     * </p>
     *
     * @param sessionId 会话 ID
     * @param userId    当前操作者 ID
     * @param userType  操作者类型：1-用户 2-商家 3-客服
     */
    @Transactional
    public void markMessagesAsRead(Long sessionId, Long userId, Integer userType) {
        List<ChatMessage> messages = chatMessageMapper.selectBySessionId(sessionId);
        LocalDateTime now = LocalDateTime.now();
        for (ChatMessage message : messages) {
            if ((userType == 1 && (message.getSenderType() == 2 || message.getSenderType() == 3)) ||
                (userType == 2 && message.getSenderType() == 1) ||
                (userType == 3 && (message.getSenderType() == 1 || message.getSenderType() == 2))) {
                if (message.getIsRead() == null || message.getIsRead() == 0) {
                    message.setIsRead(1);
                    message.setReadAt(now);
                    chatMessageMapper.updateById(message);
                }
            }
        }

        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            if (userType == 1) {
                session.setUserUnread(0);
            } else {
                session.setSellerUnread(0);
            }
            chatSessionMapper.updateById(session);
        }
    }

    public ChatSession getSessionDetail(Long sessionId) {
        return chatSessionMapper.selectById(sessionId);
    }

    /** 根据用户和商家查找对应的会话ID */
    public Long findSessionIdByUserIdAndSellerId(Long userId, Long sellerId) {
        List<ChatSession> sessions = chatSessionMapper.selectByUserId(userId);
        for (ChatSession session : sessions) {
            if (session.getSellerId() != null && session.getSellerId().equals(sellerId)) {
                return session.getId();
            }
        }
        return null;
    }

    /**
     * 强制创建新会话（**事务**，与 getOrCreateSession 不同，本方法不查重）
     *
     * @param userId    用户 ID
     * @param sellerId  商家 ID
     * @param productId 关联商品 ID（可空）
     * @param orderId   关联订单 ID（可空）
     * @return 新会话 ID
     */
    @Transactional
    public Long createSession(Long userId, Long sellerId, Long productId, Long orderId) {
        ChatSession newSession = new ChatSession();
        newSession.setUserId(userId);
        newSession.setSellerId(sellerId);
        newSession.setProductId(productId);
        newSession.setOrderId(orderId);
        newSession.setStatus(0);
        newSession.setSessionType(1);
        newSession.setAutoReplyEnabled(1);
        newSession.setUserUnread(0);
        newSession.setSellerUnread(0);
        newSession.setLastMessageAt(LocalDateTime.now());
        newSession.setCreatedAt(LocalDateTime.now());
        newSession.setUpdatedAt(LocalDateTime.now());

        chatSessionMapper.insert(newSession);
        return newSession.getId();
    }

    /**
     * 关闭会话并记录关闭原因（**事务**）
     * <p>status 置为 2（已关闭）</p>
     *
     * @param sessionId   会话 ID
     * @param closeReason 关闭原因
     */
    @Transactional
    public void closeSession(Long sessionId, String closeReason) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setStatus(2);
            session.setClosedAt(LocalDateTime.now());
            session.setCloseReason(closeReason);
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }

    /**
     * 给会话分配客服坐席，并把状态置为"已接入"（1）
     *
     * @param sessionId 会话 ID
     * @param agentId   客服坐席 ID
     */
    @Transactional
    public void assignAgent(Long sessionId, Long agentId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setAgentId(agentId);
            session.setStatus(1);
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }

}
