package com.example.minimall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek 大模型对接配置类。
 * <p>
 * 通过 {@code @ConfigurationProperties(prefix = "deepseek")} 绑定 application.yml 中
 * 的 deepseek.* 配置项，提供调用 DeepSeek API 所需的密钥、地址、模型名、温度和最大 Token 数。
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {
    private String apiKey;
    private String apiUrl;
    private String model;
    private Double temperature;
    private Integer maxTokens;

    // getter and setter
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
}