package com.example.minimall.service.impl;

import com.example.minimall.model.ChatMessage;
import com.example.minimall.service.RedisChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 基于Redis的聊天在线状态、离线消息与未读计数服务实现 */
@Service
public class RedisChatServiceImpl implements RedisChatService {

    private static final Logger logger = LoggerFactory.getLogger(RedisChatServiceImpl.class);

    /** Redis 字符串模板 */
    @Autowired
    private StringRedisTemplate redisTemplate;

    /** JSON 序列化工具 */
    @Autowired
    private ObjectMapper objectMapper;

    private static final String ONLINE_USER_PREFIX = "chat:online:user:";
    private static final String ONLINE_SELLER_PREFIX = "chat:online:seller:";
    private static final String ONLINE_AGENT_PREFIX = "chat:online:agent:";
    private static final String OFFLINE_MSG_PREFIX = "chat:offline:message:";
    private static final String UNREAD_COUNT_PREFIX = "chat:unread:";
    private static final String SESSION_ACTIVE_PREFIX = "chat:session:active:";
    private static final String LAST_MESSAGE_PREFIX = "chat:last:message:";
    private static final String AGENT_STATUS_PREFIX = "chat:agent:status:";

    @Override
    public void userOnline(Long userId) {
        try {
            String key = ONLINE_USER_PREFIX + userId;
            redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.MINUTES);
            logger.info("用户上线: userId={}", userId);
        } catch (Exception e) {
            logger.error("设置用户在线状态失败", e);
        }
    }

    @Override
    /** 标记商家上线（写入 Redis，TTL 30 分钟） */
    public void sellerOnline(Long sellerId) {
        try {
            String key = ONLINE_SELLER_PREFIX + sellerId;
            redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.MINUTES);
            logger.info("商家上线: sellerId={}", sellerId);
        } catch (Exception e) {
            logger.error("设置商家在线状态失败", e);
        }
    }

    @Override
    /** 标记客服上线 */
    public void agentOnline(Long agentId) {
        try {
            String key = ONLINE_AGENT_PREFIX + agentId;
            redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.MINUTES);
            logger.info("客服上线: agentId={}", agentId);
        } catch (Exception e) {
            logger.error("设置客服在线状态失败", e);
        }
    }

    @Override
    /** 标记用户下线 */
    public void userOffline(Long userId) {
        try {
            String key = ONLINE_USER_PREFIX + userId;
            redisTemplate.delete(key);
            logger.info("用户下线: userId={}", userId);
        } catch (Exception e) {
            logger.error("删除用户在线状态失败", e);
        }
    }

    @Override
    /** 标记商家下线（删除 Redis key） */
    public void sellerOffline(Long sellerId) {
        try {
            String key = ONLINE_SELLER_PREFIX + sellerId;
            redisTemplate.delete(key);
            logger.info("商家下线: sellerId={}", sellerId);
        } catch (Exception e) {
            logger.error("删除商家在线状态失败", e);
        }
    }

    @Override
    public void agentOffline(Long agentId) {
        try {
            String key = ONLINE_AGENT_PREFIX + agentId;
            redisTemplate.delete(key);
            String statusKey = AGENT_STATUS_PREFIX + agentId;
            redisTemplate.opsForValue().set(statusKey, "0");
            logger.info("客服下线: agentId={}", agentId);
        } catch (Exception e) {
            logger.error("删除客服在线状态失败", e);
        }
    }

    @Override
    /** 设置客服工作状态 */
    public void setAgentStatus(Long agentId, Integer status) {
        try {
            String key = AGENT_STATUS_PREFIX + agentId;
            redisTemplate.opsForValue().set(key, String.valueOf(status));
        } catch (Exception e) {
            logger.error("设置客服状态失败", e);
        }
    }

    @Override
    /** 获取客服状态 */
    public Integer getAgentStatus(Long agentId) {
        try {
            String key = AGENT_STATUS_PREFIX + agentId;
            String status = redisTemplate.opsForValue().get(key);
            return status != null ? Integer.parseInt(status) : 0;
        } catch (Exception e) {
            logger.error("获取客服状态失败", e);
            return 0;
        }
    }

    @Override
    /** 判断用户是否在线 */
    public boolean isUserOnline(Long userId) {
        try {
            String key = ONLINE_USER_PREFIX + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("检查用户在线状态失败", e);
            return false;
        }
    }

    @Override
    /** 判断商家是否在线（异常时返回 false） */
    public boolean isSellerOnline(Long sellerId) {
        try {
            String key = ONLINE_SELLER_PREFIX + sellerId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("检查商家在线状态失败", e);
            return false;
        }
    }

    @Override
    public boolean isAgentOnline(Long agentId) {
        try {
            String key = ONLINE_AGENT_PREFIX + agentId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("检查客服在线状态失败", e);
            return false;
        }
    }

    @Override
    /**
     * 保存离线消息（**仅保留最近 100 条**，TTL 7 天）
     * <p>使用 Redis List 存储，左进右出，超出后通过 trim 截断</p>
     */
    public void saveOfflineMessage(Long receiverId, ChatMessage message) {
        try {
            String key = OFFLINE_MSG_PREFIX + receiverId;
            String messageJson = objectMapper.writeValueAsString(message);

            redisTemplate.opsForList().leftPush(key, messageJson);
            redisTemplate.opsForList().trim(key, 0, 99);

            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.expire(key, 7, TimeUnit.DAYS);
            }

            logger.info("存储离线消息: receiverId={}, messageId={}", receiverId, message.getId());
        } catch (JsonProcessingException e) {
            logger.error("序列化离线消息失败", e);
        } catch (Exception e) {
            logger.error("存储离线消息失败", e);
        }
    }

    @Override
    /** 获取离线消息 */
    public List<ChatMessage> getOfflineMessages(Long receiverId) {
        try {
            String key = OFFLINE_MSG_PREFIX + receiverId;
            List<String> messageJsons = redisTemplate.opsForList().range(key, 0, -1);

            if (messageJsons == null || messageJsons.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            return messageJsons.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, ChatMessage.class);
                    } catch (JsonProcessingException e) {
                        logger.error("反序列化离线消息失败", e);
                        return null;
                    }
                })
                .filter(msg -> msg != null)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("获取离线消息失败", e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    /** 清除指定用户的离线消息 */
    public void clearOfflineMessages(Long receiverId) {
        try {
            String key = OFFLINE_MSG_PREFIX + receiverId;
            redisTemplate.delete(key);
            logger.info("清除离线消息: receiverId={}", receiverId);
        } catch (Exception e) {
            logger.error("清除离线消息失败", e);
        }
    }

    @Override
    /**
     * 增加会话未读计数（**TTL 24 小时**）
     * <p>key = chat:unread:{sessionId}:{userId}</p>
     */
    public void incrementUnreadCount(Long sessionId, Long userId) {
        try {
            String key = UNREAD_COUNT_PREFIX + sessionId + ":" + userId;
            redisTemplate.opsForValue().increment(key);

            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.expire(key, 24, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            logger.error("增加未读计数失败", e);
        }
    }

    @Override
    /** 获取会话未读计数（异常时返回 0） */
    public int getUnreadCount(Long sessionId, Long userId) {
        try {
            String key = UNREAD_COUNT_PREFIX + sessionId + ":" + userId;
            String count = redisTemplate.opsForValue().get(key);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (Exception e) {
            logger.error("获取未读计数失败", e);
            return 0;
        }
    }

    @Override
    public void clearUnreadCount(Long sessionId, Long userId) {
        try {
            String key = UNREAD_COUNT_PREFIX + sessionId + ":" + userId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.error("清除未读计数失败", e);
        }
    }

    @Override
    public void updateSessionActiveTime(Long sessionId) {
        try {
            String key = SESSION_ACTIVE_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), 7, TimeUnit.DAYS);
        } catch (Exception e) {
            logger.error("更新会话活跃时间失败", e);
        }
    }

    @Override
    /** 缓存会话最后一条消息 */
    public void cacheLastMessage(Long sessionId, String content) {
        try {
            String key = LAST_MESSAGE_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, content, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            logger.error("缓存最后消息失败", e);
        }
    }

    @Override
    /** 获取会话最后一条消息 */
    public String getLastMessage(Long sessionId) {
        try {
            String key = LAST_MESSAGE_PREFIX + sessionId;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("获取最后消息失败", e);
            return null;
        }
    }

    @Override
    /**
     * 批量获取指定用户在多个会话的未读计数
     * <p>异常时返回空 Map</p>
     */
    public Map<Long, Integer> getBatchUnreadCounts(List<Long> sessionIds, Long userId) {
        try {
            return sessionIds.stream()
                .collect(Collectors.toMap(
                    sessionId -> sessionId,
                    sessionId -> getUnreadCount(sessionId, userId)
                ));
        } catch (Exception e) {
            logger.error("批量获取未读计数失败", e);
            return java.util.Collections.emptyMap();
        }
    }
}
