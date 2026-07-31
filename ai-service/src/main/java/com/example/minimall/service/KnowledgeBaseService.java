package com.example.minimall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.config.RagConfig;
import com.example.minimall.mapper.KnowledgeDocumentMapper;
import com.example.minimall.mapper.KnowledgeFaqMapper;
import com.example.minimall.model.KnowledgeDocument;
import com.example.minimall.model.KnowledgeFaq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库管理服务。
 * <p>
 * 负责知识文档和 FAQ 的全生命周期管理，包括：
 * <ol>
 *   <li>文档 CRUD：创建、更新、删除、查询</li>
 *   <li>自动向量化：文档创建/更新时自动分块 + embedding + 存储</li>
 *   <li>FAQ CRUD：高频问答对的创建与向量索引</li>
 *   <li>知识库状态管理：启用/禁用、重新向量化</li>
 * </ol>
 * </p>
 */
@Service
public class KnowledgeBaseService {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeFaqMapper faqMapper;
    private final DocumentProcessor documentProcessor;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final RagConfig ragConfig;

    public KnowledgeBaseService(KnowledgeDocumentMapper documentMapper, KnowledgeFaqMapper faqMapper,
                                DocumentProcessor documentProcessor, EmbeddingService embeddingService,
                                VectorStoreService vectorStoreService, RagConfig ragConfig) {
        this.documentMapper = documentMapper;
        this.faqMapper = faqMapper;
        this.documentProcessor = documentProcessor;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.ragConfig = ragConfig;
    }

    // ==================== 文档管理 ====================

    /**
     * 创建知识文档并自动向量化
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocument createDocument(String title, String content, Integer sourceType,
                                            String category, String tags, Long createdBy) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setSourceType(sourceType != null ? sourceType : 0);
        doc.setCategory(category);
        doc.setTags(tags);
        doc.setStatus(0); // 待处理
        doc.setChunkCount(0);
        doc.setCreatedBy(createdBy);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(doc);
        logger.info("创建知识文档: id={}, title={}", doc.getId(), title);

        // 自动向量化
        vectorizeDocument(doc.getId());
        return doc;
    }

    /**
     * 更新知识文档（重新分块 + 向量化）
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocument updateDocument(Long id, String title, String content,
                                            String category, String tags) {
        KnowledgeDocument doc = documentMapper.selectById(id);
        if (doc == null) throw new IllegalArgumentException("文档不存在: " + id);

        if (title != null) doc.setTitle(title);
        if (content != null) doc.setContent(content);
        if (category != null) doc.setCategory(category);
        if (tags != null) doc.setTags(tags);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        // 重新向量化
        if (content != null) {
            vectorizeDocument(id);
        }
        logger.info("更新知识文档: id={}", id);
        return doc;
    }

    /**
     * 删除知识文档（级联删除分块）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        vectorStoreService.deleteChunkEmbeddings(id);
        documentMapper.deleteById(id);
        vectorStoreService.refreshCache();
        logger.info("删除知识文档: id={}", id);
    }

    /**
     * 文档向量化：分块 → embedding → 存储
     */
    @Transactional(rollbackFor = Exception.class)
    public void vectorizeDocument(Long documentId) {
        KnowledgeDocument doc = documentMapper.selectById(documentId);
        if (doc == null) throw new IllegalArgumentException("文档不存在: " + documentId);
        if (doc.getContent() == null || doc.getContent().trim().isEmpty()) {
            logger.warn("文档内容为空，跳过向量化: id={}", documentId);
            return;
        }

        // 先删除旧分块
        vectorStoreService.deleteChunkEmbeddings(documentId);

        // 分块
        List<String> chunks = documentProcessor.chunk(doc.getContent());
        if (chunks.isEmpty()) {
            logger.warn("分块结果为空: id={}", documentId);
            return;
        }
        logger.info("文档分块完成: id={}, chunks={}", documentId, chunks.size());

        // 批量 embedding
        List<float[]> vectors = embeddingService.embedBatch(chunks);

        // 存储分块 + 向量
        vectorStoreService.storeChunkEmbeddingsBatch(documentId, chunks, vectors);

        // 更新文档状态
        documentMapper.updateChunkCountAndStatus(documentId, chunks.size(), 2); // 已启用
        vectorStoreService.refreshCache();
        logger.info("文档向量化完成: id={}, chunks={}, model={}", documentId, chunks.size(),
                embeddingService.getModelName());
    }

    /**
     * 批量向量化所有待处理文档
     */
    @Transactional(rollbackFor = Exception.class)
    public int vectorizeAllPending() {
        List<KnowledgeDocument> pending = documentMapper.selectByStatus(0);
        int count = 0;
        for (KnowledgeDocument doc : pending) {
            try {
                vectorizeDocument(doc.getId());
                count++;
            } catch (Exception e) {
                logger.error("向量化文档失败: id={}, error={}", doc.getId(), e.getMessage());
            }
        }
        logger.info("批量向量化完成: {}/{} 成功", count, pending.size());
        return count;
    }

    /**
     * 启用/禁用文档
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDocumentStatus(Long id, int status) {
        documentMapper.updateChunkCountAndStatus(id,
                documentMapper.selectById(id).getChunkCount(), status);
        vectorStoreService.refreshCache();
    }

    /**
     * 获取文档详情
     */
    public KnowledgeDocument getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    /**
     * 分页查询文档
     */
    public IPage<KnowledgeDocument> listDocuments(int page, int size, String category, Integer status) {
        Page<KnowledgeDocument> pageQuery = new Page<>(page, size);
        // 简化：使用 selectByStatus 或 selectByCategory
        if (status != null) {
            List<KnowledgeDocument> docs = documentMapper.selectByStatus(status);
            int start = (int) Math.min((page - 1) * size, docs.size());
            int end = (int) Math.min(start + size, docs.size());
            pageQuery.setRecords(docs.subList(start, end));
            pageQuery.setTotal(docs.size());
        } else {
            // 全量查询
            List<KnowledgeDocument> all = documentMapper.selectList(null);
            int start = (int) Math.min((page - 1) * size, all.size());
            int end = (int) Math.min(start + size, all.size());
            pageQuery.setRecords(all.subList(start, end));
            pageQuery.setTotal(all.size());
        }
        return pageQuery;
    }

    /**
     * 搜索文档
     */
    public List<KnowledgeDocument> searchDocuments(String keyword) {
        return documentMapper.searchByTitle(keyword);
    }

    // ==================== FAQ 管理 ====================

    /**
     * 创建 FAQ 并自动向量化问题
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeFaq createFaq(String question, String answer, String category,
                                  String keywords, Integer priority, Long createdBy) {
        KnowledgeFaq faq = new KnowledgeFaq();
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setCategory(category);
        faq.setKeywords(keywords);
        faq.setPriority(priority != null ? priority : 0);
        faq.setHitCount(0);
        faq.setStatus(1);
        faq.setCreatedBy(createdBy);
        faq.setCreatedAt(LocalDateTime.now());
        faq.setUpdatedAt(LocalDateTime.now());
        faqMapper.insert(faq);

        // 向量化问题
        float[] vector = embeddingService.embed(question);
        vectorStoreService.storeFaqEmbedding(faq.getId(), question, vector);
        faq.setEmbeddingModel(embeddingService.getModelName());
        faqMapper.updateById(faq);

        logger.info("创建FAQ: id={}, question={}", faq.getId(), question);
        return faq;
    }

    /**
     * 更新 FAQ（重新向量化）
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeFaq updateFaq(Long id, String question, String answer,
                                  String category, String keywords, Integer priority) {
        KnowledgeFaq faq = faqMapper.selectById(id);
        if (faq == null) throw new IllegalArgumentException("FAQ不存在: " + id);

        boolean needReembed = false;
        if (question != null && !question.equals(faq.getQuestion())) {
            faq.setQuestion(question);
            needReembed = true;
        }
        if (answer != null) faq.setAnswer(answer);
        if (category != null) faq.setCategory(category);
        if (keywords != null) faq.setKeywords(keywords);
        if (priority != null) faq.setPriority(priority);
        faq.setUpdatedAt(LocalDateTime.now());
        faqMapper.updateById(faq);

        if (needReembed) {
            float[] vector = embeddingService.embed(question);
            vectorStoreService.storeFaqEmbedding(id, question, vector);
        }
        logger.info("更新FAQ: id={}", id);
        return faq;
    }

    /**
     * 删除 FAQ
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFaq(Long id) {
        faqMapper.deleteById(id);
        vectorStoreService.refreshFaqCache();
        logger.info("删除FAQ: id={}", id);
    }

    /**
     * 分页查询 FAQ
     */
    public IPage<KnowledgeFaq> listFaqs(int page, int size, String category) {
        Page<KnowledgeFaq> pageQuery = new Page<>(page, size);
        List<KnowledgeFaq> all;
        if (category != null && !category.isEmpty()) {
            all = faqMapper.selectByCategory(category);
        } else {
            all = faqMapper.selectList(null);
        }
        int start = (int) Math.min((page - 1) * size, all.size());
        int end = (int) Math.min(start + size, all.size());
        pageQuery.setRecords(all.subList(start, end));
        pageQuery.setTotal(all.size());
        return pageQuery;
    }

    /**
     * 获取所有启用的 FAQ
     */
    public List<KnowledgeFaq> getAllEnabledFaqs() {
        return faqMapper.selectAllEnabled();
    }

    // ==================== 统计信息 ====================

    public int getDocumentCount() {
        return documentMapper.selectList(null).size();
    }

    public int getFaqTotalCount() {
        return faqMapper.selectList(null).size();
    }

    public int getVectorizedChunkCount() {
        return vectorStoreService.getChunkCount();
    }
}
