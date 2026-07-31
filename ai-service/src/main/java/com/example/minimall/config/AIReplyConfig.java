package com.example.minimall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 自动回复配置类。
 * <p>
 * 用于集中管理客服场景下的关键词-回复模板映射，支持在 Spring 容器中以单例方式注入，
 * 业务层（如 AI 客服服务）可通过 {@link #getReplyTemplates()} 读取或修改模板。
 * </p>
 */
@Configuration
@Component
public class AIReplyConfig {

    private Map<String, String> replyTemplates = new HashMap<>();

    /**
     * 获取 AI 回复模板集合。
     */
    public Map<String, String> getReplyTemplates() {
        return replyTemplates;
    }

    /**
     * 设置 AI 回复模板集合。
     *
     * @param replyTemplates 关键词-模板映射
     */
    public void setReplyTemplates(Map<String, String> replyTemplates) {
        this.replyTemplates = replyTemplates;
    }
}
