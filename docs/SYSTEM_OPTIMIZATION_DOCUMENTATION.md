# RAG 系统优化架构设计文档

> 7 大系统问题修复与优化的架构设计、API 文档和操作手册

---

## 一、架构设计

### 1.1 优化后系统架构

```
用户查询
    │
    ▼
┌─────────────────────┐
│  AIController       │  ← 7个监控/诊断API端点
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  AIService          │
│  ┌───────────────┐  │
│  │①意图识别层    │──┼──→ IntentClassifierService (问题7)
│  │②内容过滤层    │──┼──→ ContentFilterService (问题3)
│  │③上下文优化层  │──┼──→ ProductContextOptimizer (问题6)
│  │④RAG检索层     │──┼──→ RagService + VectorStoreService (问题1)
│  │⑤监控埋点层    │──┼──→ RagMonitorService (问题5)
│  └───────────────┘  │
│  ⑥冷启动匹配层      │──→ SeedFAQInitializer (问题4)
│  ⑦Embedding层       │──→ EmbeddingService (问题2)
└─────────────────────┘
```

### 1.2 核心模块清单

| 模块 | 文件 | 职责 |
|------|------|------|
| HnswIndex | HnswIndex.java | HNSW 多层图索引算法 |
| VectorStoreService | VectorStoreService.java | 向量存储与检索（双路径） |
| EmbeddingService | EmbeddingService.java | 文本向量化（增强TF-IDF/外部API） |
| ContentFilterService | ContentFilterService.java | 3层敏感信息过滤 |
| IntentClassifierService | IntentClassifierService.java | 5种意图分类+路由 |
| ProductContextOptimizer | ProductContextOptimizer.java | 商品上下文排序+Token控制 |
| RagMonitorService | RagMonitorService.java | Micrometer指标埋点 |
| SeedFAQInitializer | SeedFAQInitializer.java | 种子FAQ+覆盖率监控 |

---

## 二、API 文档

### 2.1 监控与诊断 API

#### GET /api/ai/monitor/dashboard

系统监控仪表盘，返回核心指标、意图分布、高频查询。

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "dashboard": {
      "totalQueries": 1000,
      "totalLlmCalls": 950,
      "totalLlmSuccess": 920,
      "totalRagHits": 800,
      "totalRagMisses": 200,
      "hitRate": 0.80,
      "llmSuccessRate": 0.97,
      "healthScore": 85.5,
      "avgRetrievalTimeMs": 15.2,
      "avgEmbeddingTimeMs": 8.5,
      "intentDistribution": { "PRODUCT_QUERY": { "count": 500, "percentage": 0.50 } },
      "recentQueries": ["退货流程", "物流查询"]
    },
    "topQueries": [
      { "query": "退货流程", "count": 50 }
    ],
    "intentStatistics": { "PRODUCT_QUERY": 500, "FAQ_CONSULT": 300 }
  }
}
```

#### GET /api/ai/monitor/vector-search

向量检索策略信息（HNSW 索引状态）。

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "searchStrategy": {
      "chunkIndex": { "totalVectors": 5000, "hnswEnabled": true, "threshold": 1000 },
      "faqIndex": { "totalVectors": 50, "hnswEnabled": false, "threshold": 1000 }
    },
    "embeddingModel": "local-enhanced-tfidf-1024",
    "embeddingDimension": 1024,
    "usingExternalApi": false,
    "lexiconInfo": {
      "domainLexiconSize": 85,
      "synonymEntries": 26,
      "semanticSlots": 5,
      "dimension": 1024
    }
  }
}
```

#### GET /api/ai/monitor/embedding-quality

Embedding 语义质量自测。

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "model": "local-enhanced-tfidf-1024",
    "dimension": 1024,
    "positiveAvg": 0.65,
    "negativeAvg": 0.35,
    "discrimination": 0.30,
    "passed": true,
    "positivePairs": [
      { "text1": "退货流程是怎样的", "text2": "售后退换货政策", "similarity": 0.72 }
    ],
    "negativePairs": [
      { "text1": "苹果手机", "text2": "水果苹果", "similarity": 0.38 }
    ]
  }
}
```

#### GET /api/ai/monitor/intents

意图分类统计。

#### GET /api/ai/monitor/coverage

知识库覆盖率详情（问题4冷启动监控）。

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "coverageRate": 1.0,
    "totalKeywords": 29,
    "coveredCount": 29,
    "uncoveredCount": 0,
    "needAlert": false,
    "faqCount": 50,
    "alertThreshold": 0.70
  }
}
```

#### GET /api/ai/monitor/filter-audit

内容过滤审计日志（问题3）。

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "timestamp": 1720500000000,
      "filterType": "PII_PHONE",
      "originalSnippet": "13812345678",
      "filteredSnippet": "[REDACTED_PHONE]",
      "position": 15
    }
  ]
}
```

#### POST /api/ai/monitor/init-seed-faqs

手动触发种子FAQ初始化。

### 2.2 业务 API（已优化）

| API | 方法 | 优化内容 |
|-----|------|---------|
| /api/ai/query | POST | 集成意图识别+内容过滤+上下文优化+监控 |
| /api/ai/chat | POST(SSE) | 流式集成意图识别+内容过滤+上下文优化 |
| /api/ai/rag-query | POST | RAG+多级匹配+监控埋点 |
| /api/ai/rag-chat | POST(SSE) | 流式RAG+意图识别+内容过滤+监控 |

---

## 三、配置参数

### 3.1 application.yml 配置项

```yaml
# RAG 配置
rag:
  top-k: 5
  faq-top-k: 3
  similarity-threshold: 0.3
  max-context-tokens: 3000

# Embedding 配置
embedding:
  api-url: ""           # 外部Embedding API地址（留空使用本地模式）
  api-key: ""           # API密钥
  model: "text-embedding-3-small"
  dimensions: 1536
  timeout: 15000
  batch-size: 32

# 商品上下文优化配置
ai:
  product-context:
    max-items: 15        # 上下文最大商品数
    max-desc-length: 60  # 商品描述最大长度

# 监控配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 3.2 HNSW 参数（代码内常量）

| 参数 | 值 | 说明 |
|------|------|------|
| HNSW_THRESHOLD | 1000 | 启用HNSW索引的数据量阈值 |
| HNSW_M | 16 | 每层最大连接数 |
| HNSW_EF_CONSTRUCTION | 200 | 构建时搜索宽度 |
| HNSW_EF_SEARCH | 64 | 检索时搜索宽度 |

---

## 四、操作手册

### 4.1 启用外部 Embedding API（推荐生产环境）

```bash
# 设置环境变量
export EMBEDDING_API_URL=https://api.openai.com/v1/embeddings
export EMBEDDING_API_KEY=sk-your-api-key
export EMBEDDING_MODEL=text-embedding-3-small
export EMBEDDING_DIMENSIONS=1536
```

或在 application.yml 中配置。配置后系统自动使用外部API，语义精度提升至 0.85/0.4 标准。

### 4.2 监控仪表盘使用

1. **访问仪表盘**：`GET /api/ai/monitor/dashboard`
2. **查看HNSW状态**：`GET /api/ai/monitor/vector-search`
3. **检查Embedding质量**：`GET /api/ai/monitor/embedding-quality`
4. **查看知识库覆盖率**：`GET /api/ai/monitor/coverage`
5. **查看过滤审计日志**：`GET /api/ai/monitor/filter-audit`

### 4.3 Prometheus + Grafana 集成

1. Spring Boot 已暴露 `/actuator/prometheus` 端点
2. Prometheus 配置抓取规则：
```yaml
scrape_configs:
  - job_name: 'mall-rag'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```
3. Grafana 导入仪表盘，关注以下指标：
   - `rag_retrieval_time_seconds`（检索耗时）
   - `rag_hit_rate`（命中率）
   - `rag_health_score`（健康度）

### 4.4 冷启动处理

系统启动时自动检测知识库状态：
- 知识库为空 → 自动初始化 50+ 条种子FAQ
- 覆盖率 < 70% → 日志告警提示补充知识
- 手动触发：`POST /api/ai/monitor/init-seed-faqs`

### 4.5 敏感信息过滤配置

违禁词库在 `ContentFilterService` 中维护，包含 14 类违禁词。扩展方式：
- 修改 `FORBIDDEN_WORDS` 集合添加新词
- 或从数据库/配置文件动态加载（需自行扩展实现）

### 4.6 运行单元测试

```bash
cd backend
mvn test "-Dtest=HnswIndexTest,IntentClassifierServiceTest,ContentFilterServiceTest,EmbeddingServiceTest,ProductContextOptimizerTest,RagMonitorServiceTest,SeedFAQInitializerTest"
```

预期输出：
```
Tests run: 159, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
