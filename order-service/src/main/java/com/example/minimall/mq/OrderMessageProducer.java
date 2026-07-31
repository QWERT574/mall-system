package com.example.minimall.mq;

import com.example.minimall.mapper.MessageOutboxMapper;
import com.example.minimall.model.MessageOutbox;
import com.example.minimall.model.OrderItem;
import com.example.minimall.model.Orders;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单消息生产者 — 下单成功后发送异步消息（C5：本地消息表 + 补偿重试）。
 *
 * <p>可靠性保证：
 * <ol>
 *   <li>下单时同步写本地消息表（status=PENDING），与订单数据尽量原子</li>
 *   <li>异步发 MQ：onSuccess 标记 SENT；onException 保持 PENDING，交由补偿任务重试</li>
 *   <li>{@link MessageOutboxCompensator} 定时扫描 PENDING 记录，同步重试（指数退避）</li>
 *   <li>超过 {@link #MAX_RETRY} 次仍失败则标记 FAILED，避免无限重试</li>
 * </ol>
 *
 * <p>消息到达 Broker 后，库存服务、优惠券服务、通知服务各自消费，实现异步解耦、削峰填谷。
 */
@Slf4j
@Component
public class OrderMessageProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private MessageOutboxMapper messageOutboxMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOPIC = "ORDER_CREATED";
    private static final String BIZ_TYPE = "ORDER_CREATED";

    /** 消息状态 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_SENT = 1;
    private static final int STATUS_FAILED = 2;

    /** 最大重试次数（超过则放弃） */
    private static final int MAX_RETRY = 5;

    /**
     * 发送订单创建消息（异步）。
     *
     * <p>流程：构造消息 → 写 outbox(PENDING) → asyncSend MQ → 回调更新状态。
     * 投递失败时保持 PENDING，由 {@link MessageOutboxCompensator} 补偿重试。
     *
     * @param order 已落库的订单（需含 items）
     */
    public void sendOrderCreated(Orders order) {
        OrderMessage msg = OrderMessage.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalPrice())
                .couponId(order.getUserCouponId())
                .items(toItems(order.getItems()))
                .build();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            log.error("序列化订单消息失败: orderId={}", order.getId(), e);
            return;
        }

        // 1. 写本地消息表（PENDING）—— 紧跟订单保存，尽量原子
        MessageOutbox outbox = new MessageOutbox();
        outbox.setBizType(BIZ_TYPE);
        outbox.setBizId(order.getId());
        outbox.setTopic(TOPIC);
        outbox.setPayload(payload);
        outbox.setStatus(STATUS_PENDING);
        outbox.setRetryCount(0);
        LocalDateTime now = LocalDateTime.now();
        outbox.setNextRetryTime(now);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);
        messageOutboxMapper.insert(outbox);
        log.info("订单消息落 outbox: orderId={}, outboxId={}", order.getId(), outbox.getId());

        // 2. 异步发送 MQ（不阻塞下单主流程）
        final Long outboxId = outbox.getId();
        final Long orderId = order.getId();
        rocketMQTemplate.asyncSend(TOPIC, msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult result) {
                markSent(outboxId);
                log.info("订单消息投递成功: orderId={}, msgId={}", orderId, result.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                // 保持 PENDING，由补偿任务重试
                log.warn("订单消息投递失败,等待补偿重试: orderId={}", orderId, e);
            }
        });
    }

    /**
     * 补偿任务调用：同步重试发送一条 PENDING 消息。
     *
     * @param outbox 待重试的本地消息记录
     * @return true=发送成功（已标记 SENT）；false=失败（已更新重试计数/退避时间）
     */
    public boolean retrySend(MessageOutbox outbox) {
        try {
            OrderMessage msg = objectMapper.readValue(outbox.getPayload(), OrderMessage.class);
            rocketMQTemplate.syncSend(outbox.getTopic(), msg);
            markSent(outbox.getId());
            log.info("补偿重试成功: outboxId={}, bizId={}", outbox.getId(), outbox.getBizId());
            return true;
        } catch (Exception e) {
            log.warn("补偿重试失败: outboxId={}, retry={}/{}",
                    outbox.getId(), outbox.getRetryCount(), MAX_RETRY, e);
            updateRetryFailure(outbox);
            return false;
        }
    }

    /** 标记消息为已发送 */
    private void markSent(Long outboxId) {
        MessageOutbox update = new MessageOutbox();
        update.setId(outboxId);
        update.setStatus(STATUS_SENT);
        update.setUpdatedAt(LocalDateTime.now());
        messageOutboxMapper.updateById(update);
    }

    /** 更新重试失败状态：重试次数 +1，超限则标记 FAILED，否则设置指数退避的下次重试时间 */
    private void updateRetryFailure(MessageOutbox outbox) {
        int newRetry = (outbox.getRetryCount() == null ? 0 : outbox.getRetryCount()) + 1;
        MessageOutbox update = new MessageOutbox();
        update.setId(outbox.getId());
        update.setRetryCount(newRetry);
        update.setUpdatedAt(LocalDateTime.now());
        if (newRetry >= MAX_RETRY) {
            update.setStatus(STATUS_FAILED);
            log.error("消息超过最大重试次数,标记为 FAILED: outboxId={}, bizId={}",
                    outbox.getId(), outbox.getBizId());
        } else {
            // 指数退避：60s, 120s, 240s, 480s, 960s
            long backoffSec = 60L * (1L << (newRetry - 1));
            update.setNextRetryTime(LocalDateTime.now().plusSeconds(backoffSec));
        }
        messageOutboxMapper.updateById(update);
    }

    private List<OrderMessage.OrderItemMsg> toItems(List<OrderItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(i -> OrderMessage.OrderItemMsg.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }
}
