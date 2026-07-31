package com.example.minimall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 向量嵌入模型配置。
 * <p>
 * 支持 OpenAI 兼容的 Embedding API（如 DeepSeek Embeddings、OpenAI text-embedding-3-small、
 * 智谱 BGE 等）。当 api-url 或 api-key 为空时，自动降级为本地 TF-IDF 向量化方案，
 * 保证 RAG 管道在无外部 API 依赖时也能运行。
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingConfig {
    private String apiUrl;
    private String apiKey;
    private String model;
    private Integer dimensions;
    private Integer timeout;
    private Integer batchSize;

    /** 判断是否配置了外部 Embedding API */
    public boolean isExternalApiAvailable() {
        return apiUrl != null && !apiUrl.isEmpty()
                && apiKey != null && !apiKey.isEmpty();
    }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getDimensions() { return dimensions; }
    public void setDimensions(Integer dimensions) { this.dimensions = dimensions; }
    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }
    public Integer getBatchSize() { return batchSize; }
    public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }
}
