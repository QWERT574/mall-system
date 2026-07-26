package com.example.minimall.service;

import com.example.minimall.model.ChatMessage;

import java.util.List;
import java.util.Map;

/** 基于 Redis 的聊天在线状态与离线消息服务接口 */
public interface RedisChatService {

    /** 标记用户上线 */
    void userOnline(Long userId);

    /** 标记商家上线 */
    void sellerOnline(Long sellerId);

    /** 标记客服上线 */
    void agentOnline(Long agentId);

    /** 标记用户下线 */
    void userOffline(Long userId);

    /** 标记商家下线 */
    void sellerOffline(Long sellerId);

    /** 标记客服下线 */
    void agentOffline(Long agentId);

    /** 设置客服状态 */
    void setAgentStatus(Long agentId, Integer status);

    /** 获取客服状态 */
    Integer getAgentStatus(Long agentId);

    /** 判断用户是否在线 */
    boolean isUserOnline(Long userId);

    /** 判断商家是否在线 */
    boolean isSellerOnline(Long sellerId);

    /** 判断客服是否在线 */
    boolean isAgentOnline(Long agentId);

    /** 保存离线消息 */
    void saveOfflineMessage(Long receiverId, ChatMessage message);

    /** 获取离线消息 */
    List<ChatMessage> getOfflineMessages(Long receiverId);

    /** 清除离线消息 */
    void clearOfflineMessages(Long receiverId);

    /** 增加会话未读计数 */
    void incrementUnreadCount(Long sessionId, Long userId);

    /** 获取会话未读计数 */
    int getUnreadCount(Long sessionId, Long userId);

    /** 清除会话未读计数 */
    void clearUnreadCount(Long sessionId, Long userId);

    /** 更新会话活跃时间 */
    void updateSessionActiveTime(Long sessionId);

    /** 缓存会话最后一条消息 */
    void cacheLastMessage(Long sessionId, String content);

    /** 获取会话最后一条消息 */
    String getLastMessage(Long sessionId);

    /** 批量获取未读计数 */
    Map<Long, Integer> getBatchUnreadCounts(List<Long> sessionIds, Long userId);
}
