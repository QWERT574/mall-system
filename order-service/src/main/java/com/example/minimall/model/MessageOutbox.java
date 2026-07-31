package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表（Outbox）— 保证业务操作与消息投递的最终一致性（C5）。
 *
 * <p>状态机：PENDING(0,待发送) → SENT(1,已发送) / FAILED(2,超过最大重试次数放弃)
 *
 * <p>原理：业务事务里同步写 outbox（与业务数据尽量原子），事务提交后异步发 MQ；
 * 发送失败或崩溃未发送的记录由 {@code MessageOutboxCompensator} 定时补偿重试。
 */
@Data
@TableName("message_outbox")
public class MessageOutbox {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型，如 ORDER_CREATED */
    private String bizType;

    /** 业务 ID，如 orderId */
    private Long bizId;

    /** MQ topic */
    private String topic;

    /** 消息体 JSON */
    private String payload;

    /** 0=待发送 1=已发送 2=已放弃（超过最大重试次数） */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 下次重试时间（指数退避） */
    private LocalDateTime nextRetryTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
