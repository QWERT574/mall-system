package com.example.minimall.controller;

import com.example.minimall.model.AIServiceLog;
import com.example.minimall.model.Product;
import com.example.minimall.service.AIService;
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

    public AIController(AIService aiService) {
        this.aiService = aiService;
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
