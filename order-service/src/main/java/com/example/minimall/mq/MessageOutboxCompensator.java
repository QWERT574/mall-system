package com.example.minimall.mq;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.MessageOutboxMapper;
import com.example.minimall.model.MessageOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表补偿任务（C5）— 定时扫描待发送消息并重试投递。
 *
 * <p>触发条件：{@link MessageOutbox#status} = PENDING 且 next_retry_time <= now。
 * 重试策略由 {@link OrderMessageProducer#retrySend} 处理（指数退避 + 超限放弃）。
 */
@Slf4j
@Component
public class MessageOutboxCompensator {

    private static final int STATUS_PENDING = 0;
    /** 每轮扫描批量，避免单次重试过多压垮 MQ */
    private static final int BATCH_SIZE = 50;

    @Resource
    private MessageOutboxMapper messageOutboxMapper;

    @Resource
    private OrderMessageProducer orderMessageProducer;

    /**
     * 每 30 秒扫描一次待发送消息，补偿重试。
     * fixedDelay：上一次执行结束后等 30s 再开始下一次（避免重叠）。
     */
    @Scheduled(fixedDelay = 30_000)
    public void compensate() {
        try {
            QueryWrapper<MessageOutbox> qw = new QueryWrapper<>();
            qw.eq("status", STATUS_PENDING)
              .le("next_retry_time", LocalDateTime.now())
              .orderByAsc("id")
              .last("LIMIT " + BATCH_SIZE);
            List<MessageOutbox> pending = messageOutboxMapper.selectList(qw);
            if (pending.isEmpty()) {
                return;
            }
            log.info("补偿任务扫描到 {} 条待发送消息", pending.size());
            for (MessageOutbox msg : pending) {
                try {
                    orderMessageProducer.retrySend(msg);
                } catch (Exception e) {
                    log.error("补偿重试异常: outboxId={}", msg.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("补偿任务执行异常", e);
        }
    }
}
