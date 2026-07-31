package com.example.minimall.service;

import com.example.minimall.config.RagConfig;
import com.example.minimall.mapper.KnowledgeDocumentMapper;
import com.example.minimall.model.KnowledgeDocument;
import com.example.minimall.service.VectorStoreService.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG 检索增强生成核心编排服务。
 * <p>
 * 将「检索」与「生成」两个阶段编排为完整的 RAG 管道：
 * <ol>
 *   <li><b>检索阶段</b>：将用户查询向量化 → 从知识库检索 top-k 相关分块和 FAQ</li>
 *   <li><b>增强阶段</b>：将检索结果组装为结构化上下文，注入 LLM 系统提示词</li>
 *   <li><b>生成阶段</b>：调用 LLM 生成最终回答（由 AIService 负责）</li>
 *   <li><b>溯源阶段</b>：记录知识来源，支持可解释性展示</li>
 * </ol>
 * </p>
 */
@Service
public class RagService {
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final DocumentProcessor documentProcessor;
    private final KnowledgeDocumentMapper documentMapper;
    private final RagConfig ragConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagService(EmbeddingService embeddingService, VectorStoreService vectorStoreService,
                      DocumentProcessor documentProcessor, KnowledgeDocumentMapper documentMapper,
                      RagConfig ragConfig) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.documentProcessor = documentProcessor;
        this.documentMapper = documentMapper;
        this.ragConfig = ragConfig;
    }

    /**
     * RAG 检索结果
     */
    public static class RetrievalResult {
        /** 检索到的知识分块 */
        public List<SearchResult> chunks;
        /** 检索到的 FAQ */
        public List<SearchResult> faqs;
        /** 最高相似度得分 */
        public double topScore;
        /** 检索耗时（毫秒） */
        public long retrievalTimeMs;
        /** 组装好的上下文文本（注入 LLM prompt） */
        public String contextText;
        /** 知识来源列表（用于可解释性展示） */
        public List<Map<String, Object>> sources;

        public RetrievalResult() {
            this.chunks = new ArrayList<>();
            this.faqs = new ArrayList<>();
            this.sources = new ArrayList<>();
        }
    }

    /**
     * 执行 RAG 检索阶段
     *
     * @param query 用户查询文本
     * @return 检索结果（含上下文和来源溯源信息）
     */
    public RetrievalResult retrieve(String query) {
        long startTime = System.currentTimeMillis();
        RetrievalResult result = new RetrievalResult();

        if (query == null || query.trim().isEmpty()) {
            result.retrievalTimeMs = System.currentTimeMillis() - startTime;
            return result;
        }

        try {
            // 1. 查询向量化（记录 embedding 耗时用于监控）
            long embeddingStart = System.currentTimeMillis();
            float[] queryVector = embeddingService.embed(query);
            long embeddingDuration = System.currentTimeMillis() - embeddingStart;

            // 2. 检索知识分块
            result.chunks = vectorStoreService.searchChunks(
                    queryVector, ragConfig.getTopK(), ragConfig.getSimilarityThreshold());
            logger.info("[RAG检索] 分块检索: {} 个结果, topScore={}",
                    result.chunks.size(),
                    result.chunks.isEmpty() ? 0 : result.chunks.get(0).score);

            // 3. 检索 FAQ
            result.faqs = vectorStoreService.searchFaqs(
                    queryVector, ragConfig.getFaqTopK(), ragConfig.getSimilarityThreshold());
            logger.info("[RAG检索] FAQ检索: {} 个结果", result.faqs.size());

            // 4. 记录最高得分
            double topChunkScore = result.chunks.isEmpty() ? 0 : result.chunks.get(0).score;
            double topFaqScore = result.faqs.isEmpty() ? 0 : result.faqs.get(0).score;
            result.topScore = Math.max(topChunkScore, topFaqScore);

            // 5. 组装上下文文本
            result.contextText = buildContextText(result.chunks, result.faqs);

            // 6. 构建来源溯源信息
            result.sources = buildSources(result.chunks, result.faqs);

            logger.info("[RAG检索] embedding={}ms, total={}ms", embeddingDuration, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logger.error("[RAG检索] 检索失败: {}", e.getMessage(), e);
        }

        result.retrievalTimeMs = System.currentTimeMillis() - startTime;
        logger.info("[RAG检索] 完成: {}ms, 上下文长度={}字符, 来源数={}",
                result.retrievalTimeMs,
                result.contextText != null ? result.contextText.length() : 0,
                result.sources.size());
        return result;
    }

    /**
     * 将检索结果组装为 LLM 可理解的结构化上下文文本
     */
    private String buildContextText(List<SearchResult> chunks, List<SearchResult> faqs) {
        StringBuilder sb = new StringBuilder();

        if (!faqs.isEmpty()) {
            sb.append("【高频问答参考】\n");
            for (int i = 0; i < faqs.size(); i++) {
                SearchResult faq = faqs.get(i);
                sb.append(String.format("FAQ%d (相关度:%.2f): %s\n", i + 1, faq.score, faq.content));
            }
            sb.append("\n");
        }

        if (!chunks.isEmpty()) {
            sb.append("【知识库检索结果】\n");
            for (int i = 0; i < chunks.size(); i++) {
                SearchResult chunk = chunks.get(i);
                sb.append(String.format("知识片段%d (相关度:%.2f): %s\n", i + 1, chunk.score, chunk.content));
            }
        }

        if (sb.length() == 0) {
            return "未检索到相关知识信息。";
        }
        return sb.toString().trim();
    }

    /**
     * 构建知识来源溯源信息（用于可解释性展示）
     */
    private List<Map<String, Object>> buildSources(List<SearchResult> chunks, List<SearchResult> faqs) {
        List<Map<String, Object>> sources = new ArrayList<>();

        // 批量查询文档信息，避免 N+1 查询
        java.util.Set<Long> docIds = new java.util.HashSet<>();
        for (SearchResult chunk : chunks) {
            if (chunk.documentId != null) docIds.add(chunk.documentId);
        }
        java.util.Map<Long, KnowledgeDocument> docMap = new java.util.HashMap<>();
        if (!docIds.isEmpty()) {
            try {
                List<KnowledgeDocument> docs = documentMapper.selectBatchIds(docIds);
                for (KnowledgeDocument doc : docs) {
                    docMap.put(doc.getId(), doc);
                }
            } catch (Exception e) {
                logger.warn("批量查询文档信息失败: {}", e.getMessage());
            }
        }

        for (SearchResult chunk : chunks) {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("type", "knowledge_document");
            source.put("chunkId", chunk.id);
            source.put("documentId", chunk.documentId);
            source.put("score", Math.round(chunk.score * 100) / 100.0);
            source.put("snippet", truncate(chunk.content, 150));
            KnowledgeDocument doc = docMap.get(chunk.documentId);
            if (doc != null) {
                source.put("title", doc.getTitle());
                source.put("category", doc.getCategory());
            }
            sources.add(source);
        }

        for (SearchResult faq : faqs) {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("type", "faq");
            source.put("faqId", faq.id);
            source.put("score", Math.round(faq.score * 100) / 100.0);
            source.put("snippet", truncate(faq.content, 150));
            sources.add(source);
        }

        return sources;
    }

    /**
     * 构建 RAG 增强系统提示词
     * <p>
     * 将检索到的知识上下文注入到系统提示词中，引导 LLM 基于知识库内容回答。
     * </p>
     *
     * @param serviceType       服务类型
     * @param productContext    商品上下文（原有逻辑）
     * @param ragContext        RAG 检索上下文
     * @param conversationHistory 对话历史
     */
    public String buildRagSystemPrompt(Integer serviceType, String productContext,
                                       String ragContext, String conversationHistory) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是「乡村振兴」农产品电商平台的AI智能客服助手，具备RAG（检索增强生成）能力。\n\n");

        // 注入 RAG 检索到的知识上下文
        if (ragContext != null && !ragContext.isEmpty() && !ragContext.equals("未检索到相关知识信息。")) {
            prompt.append("【知识库检索结果（请基于以下知识回答，确保准确性）】\n");
            prompt.append(ragContext).append("\n\n");
        }

        // 注入商品上下文
        if (productContext != null && !productContext.isEmpty()) {
            prompt.append("【平台商品数据】\n").append(productContext).append("\n\n");
        }

        // 注入对话历史
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            prompt.append("【对话历史】\n").append(conversationHistory).append("\n\n");
        }

        // 服务类型特定规则
        prompt.append("【回答规则】\n");
        prompt.append("1. 优先基于【知识库检索结果】中的内容回答，确保信息准确\n");
        prompt.append("2. 当知识库结果不足时，可结合【平台商品数据】补充\n");
        prompt.append("3. 回答中引用知识库内容时，可标注来源（如「根据售后政策...」）\n");
        prompt.append("4. 如果知识库和商品数据中都没有相关信息，坦诚告知并引导用户联系人工客服\n");

        switch (serviceType != null ? serviceType : 1) {
            case 1:
                prompt.append("5. 推荐商品时必须附带商品ID，格式：商品名(ID:数字)\n");
                prompt.append("6. 用自然口语化语言回答，不要使用Markdown格式\n");
                break;
            case 2:
                prompt.append("5. 根据用户订单信息查询物流状态\n");
                prompt.append("6. 用自然口语化语言回答，不要使用Markdown格式\n");
                break;
            case 3:
                prompt.append("5. 根据售后政策知识库回答售后问题\n");
                prompt.append("6. 用自然口语化语言回答，不要使用Markdown格式\n");
                break;
            default:
                prompt.append("5. 用自然口语化语言回答，不要使用Markdown格式\n");
        }

        prompt.append("\n不要使用开场白和结束语，直接回答问题。");
        return prompt.toString();
    }

    /**
     * 判断 RAG 是否应启用
     */
    public boolean isRagEnabled() {
        return ragConfig.isEnabled();
    }

    /**
     * 将检索结果序列化为 JSON（存入 conversation_message 表）
     */
    public String serializeRetrievalResult(RetrievalResult result) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("topScore", result.topScore);
            data.put("retrievalTimeMs", result.retrievalTimeMs);
            data.put("chunkCount", result.chunks.size());
            data.put("faqCount", result.faqs.size());
            data.put("sources", result.sources);
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
