package com.example.minimall.controller;

import com.example.minimall.model.AIServiceLog;
import com.example.minimall.model.Product;
import com.example.minimall.service.AIService;
import com.example.minimall.service.ContentFilterService;
import com.example.minimall.service.EmbeddingService;
import com.example.minimall.service.IntentClassifierService;
import com.example.minimall.service.RagMonitorService;
import com.example.minimall.service.SeedFAQInitializer;
import com.example.minimall.service.VectorStoreService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** AI 智能助手与日志相关接口控制器 */
@RestController
@RequestMapping("/api/ai")
public class AIController {
    /** AI 业务服务 */
    private final AIService aiService;
    /** RAG 监控服务（问题5：可观测性） */
    private final RagMonitorService ragMonitorService;
    /** 意图识别服务（问题7：动态路由） */
    private final IntentClassifierService intentClassifierService;
    /** 敏感信息过滤服务（问题3：内容安全） */
    private final ContentFilterService contentFilterService;
    /** 种子FAQ初始化器（问题4：冷启动） */
    private final SeedFAQInitializer seedFAQInitializer;
    /** 向量存储服务（问题1：HNSW 索引） */
    private final VectorStoreService vectorStoreService;
    /** Embedding 向量化服务（问题2：语义质量） */
    private final EmbeddingService embeddingService;

    public AIController(AIService aiService,
                        RagMonitorService ragMonitorService,
                        IntentClassifierService intentClassifierService,
                        ContentFilterService contentFilterService,
                        SeedFAQInitializer seedFAQInitializer,
                        VectorStoreService vectorStoreService,
                        EmbeddingService embeddingService) {
        this.aiService = aiService;
        this.ragMonitorService = ragMonitorService;
        this.intentClassifierService = intentClassifierService;
        this.contentFilterService = contentFilterService;
        this.seedFAQInitializer = seedFAQInitializer;
        this.vectorStoreService = vectorStoreService;
        this.embeddingService = embeddingService;
    }

    /** AI 助手问答接口：处理用户问题并返回结果与关联商品卡片 */
    @PostMapping("/query")
    public Map<String, Object> aiQuery(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("[DEBUG] AI Query Request: " + request);
            
            // 当请求中没有userId时，设置默认值0
            Long userId = 0L;
            if (request.get("userId") != null && !"null".equals(request.get("userId"))) {
                userId = Long.valueOf(String.valueOf(request.get("userId")));
            }
            System.out.println("[DEBUG] userId: " + userId);
            
            String query = (String) request.get("query");
            System.out.println("[DEBUG] Original query: " + query);
            
            // 获取服务类型，默认为商品查询
            Integer serviceType = request.get("serviceType") != null ? Integer.valueOf(String.valueOf(request.get("serviceType"))) : 1;
            System.out.println("[DEBUG] serviceType: " + serviceType);
            
            // 调用AI服务生成真正的智能回复
            Map<String, Object> aiResult = aiService.handleQuery(userId, query, serviceType);
            System.out.println("[DEBUG] AI Service Result: " + aiResult);
            
            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("response", aiResult.get("response"));
            data.put("logId", aiResult.get("logId"));
            
            String aiResponse = (String) aiResult.get("response");
            List<Map<String, Object>> productCards = new ArrayList<>();
            if (serviceType != null && serviceType == 1) {
                List<Product> relatedProducts = aiService.getRelatedProducts(query, aiResponse);
                for (Product product : relatedProducts) {
                    Map<String, Object> card = new HashMap<>();
                    card.put("id", product.getId());
                    card.put("name", product.getName());
                    card.put("price", product.getPrice());
                    card.put("image", product.getCover());
                    card.put("description", product.getDescription());
                    card.put("sales", product.getSales());
                    card.put("stock", product.getStock());
                    card.put("categoryId", product.getCategoryId());
                    card.put("buyUrl", "/pages/product/product?id=" + product.getId());
                    productCards.add(card);
                }
            }
            data.put("productCards", productCards);
            
            System.out.println("[DEBUG] 最终响应数据: " + data);
            return createSuccessResponse(data);
        } catch (Exception e) {
            System.out.println("[ERROR] AI查询处理失败: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("AI查询处理失败: " + e.getMessage());
        }
    }

    /** AI 助手流式聊天接口（SSE），逐字返回生成结果 */
    @PostMapping("/chat")
    public SseEmitter aiChat(@RequestBody Map<String, Object> request) {
        System.out.println("[DEBUG] AI Chat Stream Request: " + request);

        Long userId = 0L;
        if (request.get("userId") != null && !"null".equals(request.get("userId"))) {
            userId = Long.valueOf(String.valueOf(request.get("userId")));
        }

        String query = (String) request.get("query");
        Integer serviceType = request.get("serviceType") != null ?
            Integer.valueOf(String.valueOf(request.get("serviceType"))) : 1;

        System.out.println("[DEBUG] SSE Chat - userId: " + userId + ", serviceType: " + serviceType + ", query: " + query);

        return aiService.handleQueryStream(userId, query, serviceType);
    }

    // ====================== RAG 检索增强生成接口 ======================

    /**
     * RAG 增强问答接口（非流式）。
     * <p>
     * 基于知识库检索增强生成的智能问答，支持多轮对话与知识来源溯源。
     * 返回结果包含：
     * <ul>
     *   <li>response - AI 回答文本</li>
     *   <li>sources - 知识来源列表（文档分块/FAQ），用于可解释性展示</li>
     *   <li>retrievalScore - 检索最高相似度分数</li>
     *   <li>retrievalTimeMs - 检索耗时（毫秒）</li>
     *   <li>responseTimeMs - 总响应耗时（毫秒）</li>
     *   <li>sessionToken - 会话令牌（用于多轮对话续接）</li>
     *   <li>productCards - 关联商品卡片（serviceType=1 时）</li>
     * </ul>
     * </p>
     *
     * @param request 请求体：{ query, userId?, serviceType?, sessionToken? }
     */
    @PostMapping("/rag-query")
    public Map<String, Object> ragQuery(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("[DEBUG] RAG Query Request: " + request);

            Long userId = 0L;
            if (request.get("userId") != null && !"null".equals(request.get("userId"))) {
                try {
                    userId = Long.valueOf(String.valueOf(request.get("userId")));
                } catch (NumberFormatException ignore) {
                }
            }

            String query = (String) request.get("query");
            Integer serviceType = request.get("serviceType") != null ?
                Integer.valueOf(String.valueOf(request.get("serviceType"))) : 1;
            String sessionToken = request.get("sessionToken") != null ?
                String.valueOf(request.get("sessionToken")) : null;

            System.out.println("[DEBUG] RAG Query - userId: " + userId + ", serviceType: " + serviceType
                    + ", query: " + query + ", sessionToken: " + sessionToken);

            Map<String, Object> ragResult = aiService.handleRagQuery(userId, query, serviceType, sessionToken);
            System.out.println("[DEBUG] RAG Service Result - sources: " + ragResult.get("sourceCount")
                    + ", score: " + ragResult.get("retrievalScore")
                    + ", time: " + ragResult.get("responseTimeMs") + "ms");

            return createSuccessResponse(ragResult);
        } catch (Exception e) {
            System.out.println("[ERROR] RAG查询处理失败: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("RAG查询处理失败: " + e.getMessage());
        }
    }

    /**
     * RAG 增强问答接口（SSE 流式）。
     * <p>
     * 流式事件序列：
     * <ol>
     *   <li>event: retrieval - 知识来源信息（先于生成内容推送，便于前端展示溯源）</li>
     *   <li>event: token - 逐 token 推送生成的回答（打字机效果）</li>
     *   <li>event: done - 完成事件，含来源、商品卡片、sessionToken 等</li>
     *   <li>event: error - 异常事件（如有）</li>
     * </ol>
     * </p>
     *
     * @param request 请求体：{ query, userId?, serviceType?, sessionToken? }
     */
    @PostMapping("/rag-chat")
    public SseEmitter ragChat(@RequestBody Map<String, Object> request) {
        System.out.println("[DEBUG] RAG Chat Stream Request: " + request);

        Long userId = 0L;
        if (request.get("userId") != null && !"null".equals(request.get("userId"))) {
            try {
                userId = Long.valueOf(String.valueOf(request.get("userId")));
            } catch (NumberFormatException ignore) {
            }
        }

        String query = (String) request.get("query");
        Integer serviceType = request.get("serviceType") != null ?
            Integer.valueOf(String.valueOf(request.get("serviceType"))) : 1;
        String sessionToken = request.get("sessionToken") != null ?
            String.valueOf(request.get("sessionToken")) : null;

        System.out.println("[DEBUG] RAG SSE Chat - userId: " + userId + ", serviceType: " + serviceType
                + ", query: " + query + ", sessionToken: " + sessionToken);

        return aiService.handleRagQueryStream(userId, query, serviceType, sessionToken);
    }

    /** 分页查询 AI 服务调用日志 */
    @GetMapping("/logs")
    public Map<String, Object> getAILogs(@RequestParam(required = false) Long userId,
                                        @RequestParam(required = false) Integer serviceType,
                                        @RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size) {
        try {
            com.baomidou.mybatisplus.core.metadata.IPage<AIServiceLog> logs = aiService.getLogsPage(page, size, userId, serviceType);
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", logs.getTotal());
            result.put("records", logs.getRecords());
            result.put("current", logs.getCurrent());
            result.put("size", logs.getSize());
            
            return createSuccessResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取AI服务日志失败: " + e.getMessage());
        }
    }

    /** 根据 ID 查询 AI 服务日志详情 */
    @GetMapping("/logs/{id}")
    public Map<String, Object> getAILogById(@PathVariable Long id) {
        try {
            AIServiceLog log = aiService.getLogById(id);
            if (log != null) {
                return createSuccessResponse(log);
            } else {
                return createErrorResponse("AI服务日志不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取AI服务日志失败: " + e.getMessage());
        }
    }

    /** 删除单条 AI 服务日志 */
    @DeleteMapping("/logs/{id}")
    public Map<String, Object> deleteAILog(@PathVariable Long id) {
        try {
            aiService.deleteLog(id);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("删除AI服务日志失败: " + e.getMessage());
        }
    }
    
    /** 批量删除 AI 服务日志 */
    @PostMapping("/logs/batch-delete")
    public Map<String, Object> batchDeleteAILogs(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return createErrorResponse("日志ID列表不能为空");
            }
            aiService.batchDeleteLogs(ids);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("批量删除AI服务日志失败: " + e.getMessage());
        }
    }
    
    /** 按用户/服务类型条件清空 AI 服务日志 */
    @DeleteMapping("/logs/clear")
    public Map<String, Object> clearAILogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer serviceType) {
        try {
            if (userId != null) {
                aiService.clearLogsByUserId(userId);
            } else if (serviceType != null) {
                aiService.clearLogsByServiceType(serviceType);
            } else {
                aiService.clearLogs();
            }
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("清空AI服务日志失败: " + e.getMessage());
        }
    }

    // ==================== 系统监控与诊断接口（问题2/3/4/5/7）====================

    /** 系统监控仪表盘（问题5） */
    @GetMapping("/monitor/dashboard")
    public Map<String, Object> getMonitorDashboard() {
        try {
            Map<String, Object> data = new HashMap<>();
            if (ragMonitorService != null) {
                data.put("dashboard", ragMonitorService.getDashboard());
                data.put("topQueries", ragMonitorService.getTopQueries(20));
            }
            if (intentClassifierService != null) {
                data.put("intentStatistics", intentClassifierService.getIntentStatistics());
            }
            return createSuccessResponse(data);
        } catch (Exception e) {
            return createErrorResponse("获取监控仪表盘失败: " + e.getMessage());
        }
    }

    /** 向量检索策略信息（问题1：HNSW 索引状态） */
    @GetMapping("/monitor/vector-search")
    public Map<String, Object> getVectorSearchInfo() {
        try {
            Map<String, Object> data = new HashMap<>();
            if (vectorStoreService != null) {
                data.put("searchStrategy", vectorStoreService.getSearchStrategyInfo());
            }
            if (embeddingService != null) {
                data.put("embeddingModel", embeddingService.getModelName());
                data.put("embeddingDimension", embeddingService.getDimension());
                data.put("usingExternalApi", embeddingService.isUsingExternalApi());
                data.put("lexiconInfo", embeddingService.getLexiconInfo());
            }
            return createSuccessResponse(data);
        } catch (Exception e) {
            return createErrorResponse("获取向量检索信息失败: " + e.getMessage());
        }
    }

    /** Embedding 语义质量自测（问题2） */
    @GetMapping("/monitor/embedding-quality")
    public Map<String, Object> getEmbeddingQuality() {
        try {
            if (embeddingService == null) {
                return createErrorResponse("EmbeddingService 未初始化");
            }
            Map<String, Object> data = embeddingService.evaluateSemanticQuality();
            return createSuccessResponse(data);
        } catch (Exception e) {
            return createErrorResponse("Embedding 质量评估失败: " + e.getMessage());
        }
    }

    /** 意图分类统计（问题7） */
    @GetMapping("/monitor/intents")
    public Map<String, Object> getIntentStatistics() {
        try {
            Map<String, Object> data = new HashMap<>();
            if (intentClassifierService != null) {
                data = intentClassifierService.getIntentStatistics();
            }
            return createSuccessResponse(data);
        } catch (Exception e) {
            return createErrorResponse("获取意图统计失败: " + e.getMessage());
        }
    }

    /** 知识库覆盖率（问题4：冷启动监控） */
    @GetMapping("/monitor/coverage")
    public Map<String, Object> getKnowledgeCoverage() {
        try {
            Map<String, Object> data = new HashMap<>();
            if (seedFAQInitializer != null) {
                data = seedFAQInitializer.getCoverageDetails();
            }
            return createSuccessResponse(data);
        } catch (Exception e) {
            return createErrorResponse("获取知识库覆盖率失败: " + e.getMessage());
        }
    }

    /** 内容过滤审计日志（问题3） */
    @GetMapping("/monitor/filter-audit")
    public Map<String, Object> getFilterAuditLogs(@RequestParam(defaultValue = "50") int limit) {
        try {
            Map<String, Object> data = new HashMap<>();
            if (contentFilterService != null) {
                data.put("auditLogs", contentFilterService.getAuditLogs(limit));
            }
            return createSuccessResponse(data);
        } catch (Exception e) {
            return createErrorResponse("获取过滤审计日志失败: " + e.getMessage());
        }
    }

    /** 手动触发种子FAQ初始化（问题4：冷启动） */
    @PostMapping("/monitor/init-seed-faqs")
    public Map<String, Object> initSeedFAQs() {
        try {
            if (seedFAQInitializer != null) {
                seedFAQInitializer.initializeSeedFAQs();
            }
            Map<String, Object> data = new HashMap<>();
            if (seedFAQInitializer != null) {
                data = seedFAQInitializer.getCoverageDetails();
            }
            return createSuccessResponse(data);
        } catch (Exception e) {
            return createErrorResponse("种子FAQ初始化失败: " + e.getMessage());
        }
    }

    // 构建成功响应
    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

    // 构建错误响应
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 1);
        response.put("message", message);
        return response;
    }
}
