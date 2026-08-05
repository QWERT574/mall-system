package com.example.minimall.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 库存扣减消费者 — 监听 ORDER_CREATED 消息，异步扣减库存。
 *
 * <p>设计要点：
 * <ul>
 *   <li>幂等：Redis 去重 key = "inventory:dedup:{orderId}:{productId}"，TTL 24h</li>
 *   <li>失败重试：RocketMQ 自动重试，超过 5 次进 DLQ</li>
 *   <li>削峰：maxReconsumeTimes=5，控制消费线程数</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "ORDER_CREATED",
        consumerGroup = "inventory-consumer-group",
        consumeThreadMax = 10,
        maxReconsumeTimes = 5     // 最多重试 5 次，超过进 DLQ
)
public class InventoryConsumer implements RocketMQListener<OrderMessage> {

    private final StringRedisTemplate redisTemplate;

    @Resource
    private com.example.minimall.mapper.ProductMapper productMapper;

    private static final String DEDUP_PREFIX = "inventory:dedup:";
    private static final long DEDUP_TTL_SECONDS = 86400; // 24h

    @Override
    public void onMessage(OrderMessage msg) {
        log.info("收到订单消息: orderId={}, 商品数={}", msg.getOrderId(), msg.getItems().size());

        for (OrderMessage.OrderItemMsg item : msg.getItems()) {
            String dedupKey = DEDUP_PREFIX + msg.getOrderId() + ":" + item.getProductId();

            // ── 幂等检查 ──
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "1", java.time.Duration.ofSeconds(DEDUP_TTL_SECONDS));
            if (Boolean.FALSE.equals(isNew)) {
                log.warn("重复消费，跳过: orderId={}, productId={}", msg.getOrderId(), item.getProductId());
                continue;
            }

            // ── 扣减库存（先查当前库存，乐观锁扣减）──
            try {
                com.example.minimall.model.Product product = productMapper.selectById(item.getProductId());
                if (product == null) {
                    log.error("商品不存在: productId={}", item.getProductId());
                    continue;
                }
                int rows = productMapper.updateStockById(
                        item.getProductId(), product.getStock(), item.getQuantity());
                if (rows > 0) {
                    log.info("库存扣减成功: orderId={}, productId={}, qty={}",
                            msg.getOrderId(), item.getProductId(), item.getQuantity());
                } else {
                    log.warn("库存扣减失败（可能并发冲突）: orderId={}, productId={}, retrying",
                            msg.getOrderId(), item.getProductId());
                    // 清除去重标记，等待 RocketMQ 重试
                    redisTemplate.delete(dedupKey);
                    throw new RuntimeException("库存并发冲突，等待重试");
                }
            } catch (Exception e) {
                log.error("扣减库存异常: orderId={}, productId={}", msg.getOrderId(), item.getProductId(), e);
                // 清除去重标记，允许重试
                redisTemplate.delete(dedupKey);
                throw new RuntimeException("库存扣减失败，等待重试", e);
            }
        }
    }
}
