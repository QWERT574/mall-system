package com.example.minimall.service;

import com.example.minimall.config.UserContextFilter;
import com.example.minimall.feign.OrderDto;
import com.example.minimall.feign.OrderFeignClient;
import com.example.minimall.mapper.PaymentMapper;
import com.example.minimall.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付业务服务 — 处理订单支付（模拟支付，无需真实第三方支付参数）。
 *
 * <p><b>架构说明（微服务 + Feign 远程调用）</b><br>
 * 订单数据归属 order-service，本服务通过 {@link OrderFeignClient} 远程调用 order-service 内部 API
 * 完成订单查询与状态更新；支付记录写入本地 payment 表。由于订单状态更新是跨服务远程调用，
 * 无法纳入本地 {@link Transactional} 事务，采用<b>补偿模式</b>保证最终一致性：
 * <ol>
 *   <li>Feign 远程更新订单为已支付（条件更新，防并发）</li>
 *   <li>本地写入支付记录</li>
 *   <li>若支付记录写入失败 → Feign 远程回滚订单状态（补偿），避免"订单已支付但无支付记录"</li>
 * </ol>
 * </p>
 *
 * <p><b>TODO（未来强化）</b>：可引入本地消息表 + 异步重试兜底补偿失败场景，
 * 或采用 Seata TCC 保证跨服务一致性。</p>
 */
@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    /** 订单 Feign 客户端（跨域调 order-service 内部 API） */
    private final OrderFeignClient orderFeignClient;
    /** 支付记录 Mapper（payment 域表） */
    private final PaymentMapper paymentMapper;

    public PaymentService(OrderFeignClient orderFeignClient, PaymentMapper paymentMapper) {
        this.orderFeignClient = orderFeignClient;
        this.paymentMapper = paymentMapper;
    }

    /**
     * 支付订单（模拟支付）。
     * <p>流程：远程取订单 → 校验状态/归属 → 远程条件更新订单为已支付 → 本地写支付记录（失败则补偿回滚订单）。</p>
     *
     * @param orderId     订单 ID
     * @param paymentInfo 支付信息（paymentMethod / transactionId / remark，均可空）
     * @throws IllegalArgumentException 订单不存在 / 订单状态不允许支付 / 无权支付 / 并发重复支付
     */
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, Map<String, Object> paymentInfo) {
        logger.info("Pay order request: orderId={}, paymentInfo={}", orderId, paymentInfo);

        // 1. 远程取订单（跨域调 order-service）
        OrderDto order = orderFeignClient.getOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        // 2. 授权校验：仅订单归属人可支付（防越权支付他人订单）
        Long currentUserId = UserContextFilter.getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalArgumentException("未登录或登录态失效");
        }
        if (order.getUserId() == null || !order.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("无权支付该订单");
        }

        // 3. 状态预检（仅待支付 status=0 可付；最终以远程条件更新为准，防并发）
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new IllegalArgumentException("订单状态不允许支付");
        }

        // 4. 远程条件更新订单状态：0→1、pay_status 0→1（跨服务原子操作，防并发重复支付）
        boolean updated = orderFeignClient.updateOrderToPaid(orderId);
        if (!updated) {
            // 预检通过但条件更新失败 → 并发场景，已被其他请求支付
            throw new IllegalArgumentException("订单已被支付，请勿重复支付");
        }

        // 5. 保存支付记录（payment 域表）—— 始终写流水，保证已支付订单有完整支付记录
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(order.getTotalPrice());
        payment.setStatus(1);
        payment.setPayTime(LocalDateTime.now());
        // payment_method 列为 NOT NULL，预设默认值 0（其他/未知），避免 paymentInfo 为空时插入失败
        payment.setPaymentMethod(0);
        if (paymentInfo != null) {
            // 5.1 解析支付方式
            Object paymentMethodObj = paymentInfo.get("paymentMethod");
            if (paymentMethodObj != null) {
                String methodStr = paymentMethodObj.toString();
                if ("wechat".equals(methodStr)) {
                    payment.setPaymentMethod(1);
                } else if ("alipay".equals(methodStr)) {
                    payment.setPaymentMethod(2);
                } else if ("bank".equals(methodStr)) {
                    payment.setPaymentMethod(3);
                } else {
                    payment.setPaymentMethod(0);
                }
            }
            // 5.2 记录交易号（前端传来的模拟号）
            Object transactionIdObj = paymentInfo.get("transactionId");
            if (transactionIdObj != null) {
                payment.setPaymentNo(transactionIdObj.toString());
            }
            // 5.3 备注
            Object remarkObj = paymentInfo.get("remark");
            if (remarkObj != null) {
                payment.setRemark(remarkObj.toString());
            }
        }

        // 6. 写入支付记录；失败则补偿回滚订单状态（避免订单已支付但无流水）
        try {
            paymentMapper.insert(payment);
        } catch (Exception e) {
            logger.error("支付记录写入失败，开始补偿回滚订单状态 orderId={}", orderId, e);
            try {
                boolean reverted = orderFeignClient.revertOrderPay(orderId);
                logger.warn("补偿回滚结果 orderId={}, reverted={}", orderId, reverted);
            } catch (Exception revertEx) {
                // 补偿失败属严重一致性问题，记录日志人工介入；本地事务回滚（无本地写），异常向上抛
                logger.error("补偿回滚订单失败！orderId={} 存在数据不一致风险", orderId, revertEx);
            }
            throw new IllegalArgumentException("支付记录写入失败，已尝试回滚订单");
        }

        logger.info("Order paid successfully, orderId: {}", orderId);
    }
}
