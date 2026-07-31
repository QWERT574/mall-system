package com.example.minimall.config;

import com.example.minimall.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sentinel Fallback 统一处理 — 限流/熔断触发后的兜底响应。
 *
 * <p>用法示例：
 * <pre>{@code
 * @SentinelResource(value = "createOrder",
 *     blockHandlerClass = SentinelFallbackHandler.class,
 *     blockHandler = "createOrderBlocked",
 *     fallbackClass = SentinelFallbackHandler.class,
 *     fallback = "createOrderFallback")
 * public Result createOrder(OrderDTO dto) { ... }
 * }</pre>
 *
 * <h3>blockHandler vs fallback</h3>
 * <ul>
 *   <li><b>blockHandler</b>：触发限流/熔断规则时调用（BlockException）</li>
 *   <li><b>fallback</b>：业务方法抛出异常时调用（所有 Throwable）</li>
 * </ul>
 */
@Slf4j
@Component
public class SentinelFallbackHandler {

    // ================================================================
    //  Block Handlers — 限流/熔断触发
    // ================================================================

    /**
     * 下单限流兜底。
     */
    public static Result<?> createOrderBlocked(Object dto, com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("下单被限流: {}", ex.getMessage());
        return Result.error(429, "当前下单人数过多，请稍后再试");
    }

    /**
     * AI 对话限流兜底。
     */
    public static Result<?> aiChatBlocked(String query, com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("AI 对话被限流");
        return Result.error(429, "AI 客服繁忙，请稍后再试或转人工客服");
    }

    /**
     * 秒杀限流兜底。
     */
    public static Result<?> seckillBlocked(Object dto, com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("秒杀被限流");
        return Result.error(429, "活动太火爆了，请稍后再试");
    }

    /**
     * 全局限流兜底。
     */
    public static Result<?> globalBlocked(com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("全局限流触发: {}", ex.getMessage());
        return Result.error(429, "系统繁忙，请稍后再试");
    }

    // ================================================================
    //  Fallback Handlers — 业务异常触发
    // ================================================================

    /**
     * 下单业务异常兜底。
     */
    public static Result<?> createOrderFallback(Object dto, Throwable ex) {
        log.error("下单失败: {}", ex.getMessage(), ex);
        return Result.error(500, "下单失败，请重试");
    }

    /**
     * AI 对话异常兜底。
     */
    public static Result<?> aiChatFallback(String query, Throwable ex) {
        log.error("AI 对话异常: {}", ex.getMessage());
        return Result.error(500, "AI 服务暂时不可用，已转接人工客服");
    }

    /**
     * 全局异常兜底。
     */
    public static Result<?> globalFallback(Throwable ex) {
        log.error("服务异常: {}", ex.getMessage());
        return Result.error(500, "服务暂时不可用，请稍后再试");
    }
}
