package com.example.minimall.service;

import java.util.Map;

/** 聊天会话监控与健康检查服务接口 */
public interface ChatMonitorService {

    /** 记录一条已发送的消息 */
    void recordMessageSent();

    /** 记录一条已投递的消息 */
    void recordMessageDelivered();

    /** 记录一条发送失败的消息 */
    void recordMessageFailed();

    /** 记录一条投递超时的消息 */
    void recordDeliveryTimeout();

    /** 获取监控指标 */
    Map<String, Object> getMetrics();

    /** 获取服务健康状态 */
    Map<String, Object> getHealth();

    /** 定时检查未投递消息 */
    void checkUndeliveredMessages();

    /** 周期重置计数器 */
    void resetCounters();
}
