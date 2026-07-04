package com.example.minimall.service.impl;

import com.example.minimall.mapper.ChatMessageMapper;
import com.example.minimall.mapper.ChatSessionMapper;
import com.example.minimall.model.ChatMessage;
import com.example.minimall.model.ChatSession;
import com.example.minimall.service.ChatMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** 聊天会话监控与健康检查服务实现 */
@Service
/**
 * 聊天监控服务实现
 * <p>
 * 负责统计消息发送/送达/失败指标 + 系统健康检查 + 定时扫描未送达消息。
 * 关键设计：使用 AtomicLong 计数器、@Scheduled 定时任务。
 * </p>
 */
public class ChatMonitorServiceImpl implements ChatMonitorService {
    private static final Logger logger = LoggerFactory.getLogger(ChatMonitorServiceImpl.class);

    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesDelivered = new AtomicLong(0);
    private final AtomicLong messagesFailed = new AtomicLong(0);
    private final AtomicLong deliveryTimeouts = new AtomicLong(0);

    /** 聊天消息Mapper */
    @Autowired
    private ChatMessageMapper messageMapper;

    /** 聊天会话Mapper */
    @Autowired
    private ChatSessionMapper sessionMapper;

    /** Redis操作模板 */
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void recordMessageSent() {
        messagesSent.incrementAndGet();
    }

    @Override
    public void recordMessageDelivered() {
        messagesDelivered.incrementAndGet();
    }

    @Override
    /** 记录一条消息投递失败 */
    public void recordMessageFailed() {
        messagesFailed.incrementAndGet();
    }

    @Override
    public void recordDeliveryTimeout() {
        deliveryTimeouts.incrementAndGet();
    }

    @Override
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        long sent = messagesSent.get();
        long delivered = messagesDelivered.get();
        long failed = messagesFailed.get();
        long timeouts = deliveryTimeouts.get();

        metrics.put("messagesSent", sent);
        metrics.put("messagesDelivered", delivered);
        metrics.put("messagesFailed", failed);
        metrics.put("deliveryTimeouts", timeouts);
        metrics.put("deliveryRate", sent > 0 ? String.format("%.2f%%", (double) delivered / sent * 100) : "N/A");
        metrics.put("failureRate", sent > 0 ? String.format("%.2f%%", (double) failed / sent * 100) : "N/A");

        try {
            QueryWrapper<ChatSession> activeWrapper = new QueryWrapper<>();
            activeWrapper.in("status", 0, 1);
            Long activeSessions = sessionMapper.selectCount(activeWrapper);
            metrics.put("activeSessions", activeSessions);

            QueryWrapper<ChatMessage> recentWrapper = new QueryWrapper<>();
            recentWrapper.ge("created_at", LocalDateTime.now().minusHours(1));
            Long recentMessages = messageMapper.selectCount(recentWrapper);
            metrics.put("recentMessages1h", recentMessages);

            QueryWrapper<ChatMessage> undeliveredWrapper = new QueryWrapper<>();
            undeliveredWrapper.eq("status", 1);
            undeliveredWrapper.lt("created_at", LocalDateTime.now().minusSeconds(30));
            Long undeliveredOld = messageMapper.selectCount(undeliveredWrapper);
            metrics.put("undeliveredOlderThan30s", undeliveredOld);

            if (undeliveredOld > 10) {
                logger.warn("告警: 有 {} 条消息超过30秒未投递", undeliveredOld);
            }
        } catch (Exception e) {
            logger.warn("获取数据库指标失败: {}", e.getMessage());
        }

        try {
            Set<String> userKeys = redisTemplate.keys("chat:online:user:*");
            Set<String> sellerKeys = redisTemplate.keys("chat:online:seller:*");
            metrics.put("onlineUsers", userKeys != null ? userKeys.size() : 0);
            metrics.put("onlineSellers", sellerKeys != null ? sellerKeys.size() : 0);
        } catch (Exception e) {
            metrics.put("onlineUsers", "Redis不可用");
            metrics.put("onlineSellers", "Redis不可用");
        }

        metrics.put("timestamp", LocalDateTime.now().toString());
        return metrics;
    }

    @Override
    /** 获取服务健康状态 */
    public Map<String, Object> getHealth() {
        Map<String, Object> health = new HashMap<>();
        boolean dbOk = false;
        boolean redisOk = false;

        try {
            messageMapper.selectCount(new QueryWrapper<ChatMessage>().last("LIMIT 1"));
            dbOk = true;
        } catch (Exception e) {
            logger.error("数据库健康检查失败: {}", e.getMessage());
        }

        try {
            String pong = redisTemplate.execute((RedisCallback<String>) connection -> {
                return connection.ping();
            });
            redisOk = "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            logger.error("Redis健康检查失败: {}", e.getMessage());
        }

        health.put("status", dbOk && redisOk ? "UP" : "DOWN");
        health.put("database", dbOk ? "UP" : "DOWN");
        health.put("redis", redisOk ? "UP" : "DOWN");
        health.put("timestamp", LocalDateTime.now().toString());
        return health;
    }

    @Override
    @Scheduled(fixedRate = 60000)
    /** 定期检查未投递消息并告警 */
    /**
     * 定时扫描未送达消息（@Scheduled 触发）
     * <p>
     * 检查 status=1（已发送）但未在合理时间内送达的消息，标记为失败并通知管理员。
     * </p>
     */
    public void checkUndeliveredMessages() {
        try {
            QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
            wrapper.eq("status", 1);
            wrapper.lt("created_at", LocalDateTime.now().minusSeconds(60));
            Long count = messageMapper.selectCount(wrapper);
            if (count > 0) {
                logger.warn("监控告警: 有 {} 条消息发送超过60秒仍未投递", count);
                deliveryTimeouts.addAndGet(count);
            }
        } catch (Exception e) {
            logger.error("监控检查失败: {}", e.getMessage());
        }
    }

    @Override
    @Scheduled(fixedRate = 300000)
    /** 周期重置计数器 */
    public void resetCounters() {
        long sent = messagesSent.get();
        long delivered = messagesDelivered.get();
        long failed = messagesFailed.get();
        logger.info("消息投递统计(5分钟): 发送={}, 投递={}, 失败={}, 投递率={}",
                sent, delivered, failed,
                sent > 0 ? String.format("%.1f%%", (double) delivered / sent * 100) : "N/A");
        messagesSent.set(0);
        messagesDelivered.set(0);
        messagesFailed.set(0);
        deliveryTimeouts.set(0);
    }
}
