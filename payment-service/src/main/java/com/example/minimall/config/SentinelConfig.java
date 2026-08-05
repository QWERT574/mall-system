package com.example.minimall.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 规则配置 — 代码中预设默认限流/熔断规则。
 *
 * <p>规则也可以在 Sentinel Dashboard 中动态修改（Dashboard 优先级更高）。
 * 这里预设的是"保底"规则 —— Dashboard 挂了也不影响基本保护。
 *
 * <h3>核心概念</h3>
 * <ul>
 *   <li><b>限流 (Flow)</b>：接口 QPS 超过阈值 → 直接拒绝或排队等待</li>
 *   <li><b>熔断 (Degrade)</b>：接口慢调用比例或异常比例超过阈值 → 打开断路器，
 *       一段时间内所有请求直接走 fallback，保护下游服务恢复</li>
 * </ul>
 */
@Slf4j
@Configuration
public class SentinelConfig {

    /**
     * 让 @SentinelResource 注解生效的切面。
     * 不加这个 Bean，@SentinelResource 不会拦截方法调用。
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    /**
     * 应用启动时加载默认规则。
     */
    @PostConstruct
    public void initRules() {
        initFlowRules();
        initDegradeRules();
        log.info("Sentinel 默认规则加载完成");
    }

    // ================================================================
    //  限流规则
    // ================================================================
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 下单接口 — 每秒最多 100 个请求（防刷单）
        rules.add(createFlowRule("createOrder", 100));
        // 秒杀接口 — 每秒最多 500 个请求
        rules.add(createFlowRule("seckill", 500));
        // AI 对话 — 每秒最多 20 个请求（DeepSeek API 有频率限制）
        rules.add(createFlowRule("aiChat", 20));
        // 全局兜底 — 所有接口合并 QPS 不超过 2000
        rules.add(createFlowRule("globalFallback", 2000));

        FlowRuleManager.loadRules(rules);
    }

    private FlowRule createFlowRule(String resource, int qps) {
        FlowRule rule = new FlowRule();
        rule.setResource(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);  // 按 QPS 限流
        rule.setCount(qps);
        return rule;
    }

    // ================================================================
    //  熔断降级规则
    // ================================================================
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 下单服务：慢调用比例超过 20% 且最小 5 个请求时触发熔断
        rules.add(createDegradeRule("createOrder", 0.2, 5, 1000, 10));
        // AI 服务：慢调用比例超过 30% 触发熔断
        rules.add(createDegradeRule("aiChat", 0.3, 3, 3000, 30));

        DegradeRuleManager.loadRules(rules);
    }

    /**
     * 创建熔断规则。
     *
     * @param resource      资源名（对应 @SentinelResource value）
     * @param threshold     慢调用比例阈值（0.0 ~ 1.0）
     * @param minRequests   最小请求数（低于此值不触发熔断）
     * @param maxRtMs       慢调用 RT 阈值（毫秒）
     * @param timeWindowSec 熔断恢复时间（秒）
     */
    private DegradeRule createDegradeRule(String resource, double threshold,
                                           int minRequests, int maxRtMs, int timeWindowSec) {
        DegradeRule rule = new DegradeRule();
        rule.setResource(resource);
        rule.setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType());
        rule.setCount(maxRtMs);           // RT 阈值
        rule.setSlowRatioThreshold(threshold);
        rule.setMinRequestAmount(minRequests);
        rule.setTimeWindow(timeWindowSec);
        rule.setStatIntervalMs(1000);     // 统计窗口 1s
        return rule;
    }
}
