package com.example.minimall.service;

import com.example.minimall.config.RagConfig;
import com.example.minimall.mapper.ConversationMessageMapper;
import com.example.minimall.mapper.ConversationSessionMapper;
import com.example.minimall.model.ConversationMessage;
import com.example.minimall.model.ConversationSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 多轮对话上下文管理服务。
 * <p>
 * 核心职责：
 * <ol>
 *   <li>会话管理：创建/获取/关闭对话会话</li>
 *   <li>消息持久化：记录每轮对话的完整信息（含 RAG 溯源）</li>
 *   <li>上下文构建：将历史对话格式化为 LLM 可理解的上下文文本</li>
 *   <li>上下文压缩：当对话轮数超过阈值时，自动摘要压缩旧消息</li>
 * </ol>
 * </p>
 */
@Service
public class ConversationService {
    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationSessionMapper sessionMapper;
    private final ConversationMessageMapper messageMapper;
    private final RagConfig ragConfig;

    public ConversationService(ConversationSessionMapper sessionMapper,
                               ConversationMessageMapper messageMapper, RagConfig ragConfig) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.ragConfig = ragConfig;
    }

    /**
     * 创建新的对话会话
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversationSession createSession(Long userId, Integer serviceType) {
        ConversationSession session = new ConversationSession();
        session.setSessionToken(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setServiceType(serviceType != null ? serviceType : 1);
        session.setMessageCount(0);
        session.setStatus(1);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        logger.info("创建对话会话: token={}, userId={}", session.getSessionToken(), userId);
        return session;
    }

    /**
     * 按 token 获取会话，不存在则创建
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversationSession getOrCreateSession(String sessionToken, Long userId, Integer serviceType) {
        if (sessionToken != null && !sessionToken.isEmpty()) {
            ConversationSession existing = sessionMapper.selectByToken(sessionToken);
            if (existing != null) {
                if (existing.getStatus() == 0) {
                    // 会话已关闭，创建新的
                    return createSession(userId, serviceType);
                }
                return existing;
            }
        }
        return createSession(userId, serviceType);
    }

    /**
     * 添加用户消息
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversationMessage addUserMessage(Long sessionId, String content) {
        ConversationMessage msg = new ConversationMessage();
        msg.setSessionId(sessionId);
        msg.setRole("user");
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        incrementMessageCount(sessionId);
        return msg;
    }

    /**
     * 添加 AI 回复消息（含 RAG 溯源信息）
     */
    @Transactional(rollbackFor = Exception.class)
    public ConversationMessage addAssistantMessage(Long sessionId, String content, String sources,
                                                    String retrievedChunks, String retrievedFaqs,
                                                    java.math.BigDecimal retrievalScore, Integer responseTimeMs) {
        ConversationMessage msg = new ConversationMessage();
        msg.setSessionId(sessionId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setSources(sources);
        msg.setRetrievedChunks(retrievedChunks);
        msg.setRetrievedFaqs(retrievedFaqs);
        msg.setRetrievalScore(retrievalScore);
        msg.setResponseTimeMs(responseTimeMs);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        incrementMessageCount(sessionId);
        return msg;
    }

    /**
     * 获取最近 N 轮对话历史，格式化为 LLM 上下文文本
     */
    public String buildConversationContext(Long sessionId) {
        if (!ragConfig.isMultiTurnEnabled()) return null;

        int limit = ragConfig.getConversationHistoryTurns() * 2; // 每轮 = user + assistant
        List<ConversationMessage> messages = messageMapper.selectRecentBySessionId(sessionId, limit);
        if (messages == null || messages.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        for (ConversationMessage msg : messages) {
            if ("user".equals(msg.getRole())) {
                sb.append("用户: ").append(msg.getContent()).append("\n");
            } else if ("assistant".equals(msg.getRole())) {
                // 只取回答的前200字作为上下文摘要
                String content = msg.getContent();
                if (content != null && content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                sb.append("助手: ").append(content).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 获取会话的所有消息
     */
    public List<ConversationMessage> getMessages(Long sessionId) {
        return messageMapper.selectBySessionId(sessionId);
    }

    /**
     * 获取会话详情
     */
    public ConversationSession getSession(Long id) {
        return sessionMapper.selectById(id);
    }

    /**
     * 按 token 获取会话
     */
    public ConversationSession getSessionByToken(String token) {
        return sessionMapper.selectByToken(token);
    }

    /**
     * 关闭会话
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeSession(Long sessionId) {
        sessionMapper.closeSession(sessionId);
        logger.info("关闭对话会话: id={}", sessionId);
    }

    /**
     * 获取用户的活跃会话
     */
    public List<ConversationSession> getActiveSessions(Long userId) {
        return sessionMapper.selectActiveByUserId(userId);
    }

    private void incrementMessageCount(Long sessionId) {
        ConversationSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            int newCount = (session.getMessageCount() != null ? session.getMessageCount() : 0) + 1;
            // 首条消息设置为标题
            if (newCount == 1) {
                ConversationMessage lastMsg = messageMapper.selectRecentBySessionId(sessionId, 1).isEmpty()
                        ? null : messageMapper.selectRecentBySessionId(sessionId, 1).get(0);
                if (lastMsg != null && lastMsg.getContent() != null) {
                    String title = lastMsg.getContent().length() > 50
                            ? lastMsg.getContent().substring(0, 50) + "..." : lastMsg.getContent();
                    session.setTitle(title);
                }
            }
            session.setMessageCount(newCount);
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }
}
