package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.minimall.common.Result;
import com.example.minimall.model.*;
import com.example.minimall.service.ConversationService;
import com.example.minimall.service.KnowledgeBaseService;
import com.example.minimall.service.VectorStoreService;
import com.example.minimall.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器（管理后台专用）。
 * <p>
 * 提供知识文档 CRUD、FAQ 管理、向量化操作、对话历史查看、统计信息等接口。
 * 所有接口需管理员认证（userType=2），由 PermissionInterceptor 自动校验。
 * </p>
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseController.class);

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private VectorStoreService vectorStoreService;
    @Autowired
    private JwtUtil jwtUtil;

    // ==================== 知识文档管理 ====================

    /** 创建知识文档（自动分块+向量化） */
    @PostMapping("/documents")
    public Result<KnowledgeDocument> createDocument(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long createdBy = getUserId(request);
            String title = (String) body.get("title");
            String content = (String) body.get("content");
            Integer sourceType = body.get("sourceType") != null ? Integer.valueOf(body.get("sourceType").toString()) : 0;
            String category = (String) body.get("category");
            String tags = (String) body.get("tags");
            if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
                return Result.error("标题和内容不能为空");
            }
            KnowledgeDocument doc = knowledgeBaseService.createDocument(title, content, sourceType, category, tags, createdBy);
            return Result.success(doc);
        } catch (Exception e) {
            logger.error("创建知识文档失败: {}", e.getMessage(), e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /** 更新知识文档 */
    @PutMapping("/documents/{id}")
    public Result<KnowledgeDocument> updateDocument(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            KnowledgeDocument doc = knowledgeBaseService.updateDocument(id,
                    (String) body.get("title"), (String) body.get("content"),
                    (String) body.get("category"), (String) body.get("tags"));
            return Result.success(doc);
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /** 删除知识文档 */
    @DeleteMapping("/documents/{id}")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        try {
            knowledgeBaseService.deleteDocument(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /** 获取文档详情 */
    @GetMapping("/documents/{id}")
    public Result<KnowledgeDocument> getDocument(@PathVariable Long id) {
        KnowledgeDocument doc = knowledgeBaseService.getDocument(id);
        if (doc == null) return Result.error("文档不存在");
        return Result.success(doc);
    }

    /** 分页查询文档列表 */
    @GetMapping("/documents")
    public Result<Map<String, Object>> listDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        IPage<KnowledgeDocument> result = knowledgeBaseService.listDocuments(page, size, category, status);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success(data);
    }

    /** 搜索文档 */
    @GetMapping("/documents/search")
    public Result<List<KnowledgeDocument>> searchDocuments(@RequestParam String keyword) {
        return Result.success(knowledgeBaseService.searchDocuments(keyword));
    }

    /** 重新向量化文档 */
    @PostMapping("/documents/{id}/vectorize")
    public Result<Void> vectorizeDocument(@PathVariable Long id) {
        try {
            knowledgeBaseService.vectorizeDocument(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("向量化失败: " + e.getMessage());
        }
    }

    /** 批量向量化所有待处理文档 */
    @PostMapping("/documents/vectorize-all")
    public Result<Map<String, Object>> vectorizeAll() {
        int count = knowledgeBaseService.vectorizeAllPending();
        Map<String, Object> data = new HashMap<>();
        data.put("vectorizedCount", count);
        return Result.success(data);
    }

    /** 启用/禁用文档 */
    @PutMapping("/documents/{id}/status")
    public Result<Void> setDocumentStatus(@PathVariable Long id, @RequestParam int status) {
        knowledgeBaseService.setDocumentStatus(id, status);
        return Result.success(null);
    }

    // ==================== FAQ 管理 ====================

    /** 创建FAQ */
    @PostMapping("/faqs")
    public Result<KnowledgeFaq> createFaq(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long createdBy = getUserId(request);
            String question = (String) body.get("question");
            String answer = (String) body.get("answer");
            if (question == null || question.trim().isEmpty() || answer == null || answer.trim().isEmpty()) {
                return Result.error("问题和答案不能为空");
            }
            KnowledgeFaq faq = knowledgeBaseService.createFaq(question, answer,
                    (String) body.get("category"), (String) body.get("keywords"),
                    body.get("priority") != null ? Integer.valueOf(body.get("priority").toString()) : 0,
                    createdBy);
            return Result.success(faq);
        } catch (Exception e) {
            return Result.error("创建FAQ失败: " + e.getMessage());
        }
    }

    /** 更新FAQ */
    @PutMapping("/faqs/{id}")
    public Result<KnowledgeFaq> updateFaq(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            KnowledgeFaq faq = knowledgeBaseService.updateFaq(id,
                    (String) body.get("question"), (String) body.get("answer"),
                    (String) body.get("category"), (String) body.get("keywords"),
                    body.get("priority") != null ? Integer.valueOf(body.get("priority").toString()) : null);
            return Result.success(faq);
        } catch (Exception e) {
            return Result.error("更新FAQ失败: " + e.getMessage());
        }
    }

    /** 删除FAQ */
    @DeleteMapping("/faqs/{id}")
    public Result<Void> deleteFaq(@PathVariable Long id) {
        knowledgeBaseService.deleteFaq(id);
        return Result.success(null);
    }

    /** 分页查询FAQ */
    @GetMapping("/faqs")
    public Result<Map<String, Object>> listFaqs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category) {
        IPage<KnowledgeFaq> result = knowledgeBaseService.listFaqs(page, size, category);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success(data);
    }

    // ==================== 对话历史 ====================

    /** 获取用户的对话会话列表 */
    @GetMapping("/conversations")
    public Result<List<ConversationSession>> listConversations(
            @RequestParam(required = false) Long userId) {
        if (userId != null) {
            return Result.success(conversationService.getActiveSessions(userId));
        }
        return Result.success(conversationService.getActiveSessions(null));
    }

    /** 获取会话的消息列表 */
    @GetMapping("/conversations/{sessionId}/messages")
    public Result<List<ConversationMessage>> getConversationMessages(@PathVariable Long sessionId) {
        return Result.success(conversationService.getMessages(sessionId));
    }

    /** 关闭对话会话 */
    @PostMapping("/conversations/{sessionId}/close")
    public Result<Void> closeConversation(@PathVariable Long sessionId) {
        conversationService.closeSession(sessionId);
        return Result.success(null);
    }

    // ==================== 统计信息 ====================

    /** 知识库统计信息 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("documentCount", knowledgeBaseService.getDocumentCount());
        stats.put("faqCount", knowledgeBaseService.getFaqTotalCount());
        stats.put("vectorizedChunkCount", knowledgeBaseService.getVectorizedChunkCount());
        stats.put("vectorStoreChunkCount", vectorStoreService.getChunkCount());
        stats.put("vectorStoreFaqCount", vectorStoreService.getFaqCount());
        return Result.success(stats);
    }

    private Long getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.getUserIdFromToken(authHeader.substring(7));
            } catch (Exception e) {
                logger.error("Failed to get userId from token", e);
            }
        }
        return null;
    }
}
