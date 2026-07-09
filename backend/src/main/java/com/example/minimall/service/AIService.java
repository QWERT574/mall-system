package com.example.minimall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.config.DeepSeekConfig;
import com.example.minimall.mapper.AIServiceLogMapper;
import com.example.minimall.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI 智能客服服务，负责调用 DeepSeek 大模型处理商品推荐、物流查询、售后等业务 */
@Service
public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    /** ObjectMapper 线程安全，全局复用，避免重复创建开销 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** AI 服务日志 Mapper */
    private final AIServiceLogMapper aiServiceLogMapper;
    /** DeepSeek 模型相关配置 */
    private final DeepSeekConfig deepSeekConfig;
    /** 商品服务 */
    private final ProductService productService;
    /** 分类服务 */
    private final CategoryService categoryService;
    /** 订单服务 */
    private final OrderService orderService;
    /** 物流服务 */
    private final LogisticsService logisticsService;
    /** 售后服务接口 */
    private final AfterSaleServiceApi afterSaleServiceApi;
    /** 优惠活动服务 */
    private final DiscountActivityService discountActivityService;
    /** HTTP 客户端 */
    private final RestTemplate restTemplate;
    /** RAG 检索增强生成服务 */
    private final RagService ragService;
    /** 多轮对话管理服务 */
    private final ConversationService conversationService;
    /** 意图识别服务（问题7：动态路由） */
    private final IntentClassifierService intentClassifierService;
    /** 敏感信息过滤服务（问题3：内容安全） */
    private final ContentFilterService contentFilterService;
    /** 商品上下文优化器（问题6：上下文控制） */
    private final ProductContextOptimizer productContextOptimizer;
    /** RAG 监控服务（问题5：可观测性） */
    private final RagMonitorService ragMonitorService;
    /** 种子FAQ初始化器（问题4：冷启动） */
    private final SeedFAQInitializer seedFAQInitializer;

    private final Map<String, String> queryCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AIService(AIServiceLogMapper aiServiceLogMapper,
                     DeepSeekConfig deepSeekConfig,
                     ProductService productService,
                     CategoryService categoryService,
                     OrderService orderService,
                     LogisticsService logisticsService,
                     AfterSaleServiceApi afterSaleServiceApi,
                     DiscountActivityService discountActivityService,
                     RagService ragService,
                     ConversationService conversationService,
                     IntentClassifierService intentClassifierService,
                     ContentFilterService contentFilterService,
                     ProductContextOptimizer productContextOptimizer,
                     RagMonitorService ragMonitorService,
                     SeedFAQInitializer seedFAQInitializer) {
        this.aiServiceLogMapper = aiServiceLogMapper;
        this.deepSeekConfig = deepSeekConfig;
        this.productService = productService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.logisticsService = logisticsService;
        this.afterSaleServiceApi = afterSaleServiceApi;
        this.discountActivityService = discountActivityService;
        this.ragService = ragService;
        this.conversationService = conversationService;
        this.intentClassifierService = intentClassifierService;
        this.contentFilterService = contentFilterService;
        this.productContextOptimizer = productContextOptimizer;
        this.ragMonitorService = ragMonitorService;
        this.seedFAQInitializer = seedFAQInitializer;
        this.restTemplate = createRestTemplate();
    }

    // 创建带有连接池和超时配置的RestTemplate
    private RestTemplate createRestTemplate() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .setSocketTimeout(30000)
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(10);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        factory.setConnectionRequestTimeout(10000);

        return new RestTemplate(factory);
    }

    private static final Map<String, String[]> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    static {
        CATEGORY_KEYWORDS.put("水果", new String[]{"水果", "鲜果", "fruit", "果", "苹果", "香蕉", "橙子", "草莓", "葡萄", "西瓜", "梨", "芒果", "猕猴桃", "车厘子", "樱桃", "桃子", "荔枝", "龙眼", "榴莲", "菠萝", "柚子", "柑橘", "枇杷", "蓝莓"});
        CATEGORY_KEYWORDS.put("蔬菜", new String[]{"蔬菜", "菜", "vegetable", "黄瓜", "西红柿", "生菜", "白菜", "萝卜", "菠菜", "土豆", "胡萝卜", "紫甘蓝", "茄子", "辣椒", "青椒", "豆角", "南瓜", "冬瓜", "芹菜", "韭菜", "洋葱", "莲藕"});
        CATEGORY_KEYWORDS.put("肉类", new String[]{"肉", "猪肉", "牛肉", "羊肉", "鸡肉", "排骨", "五花肉", "腊肉", "土鸡", "鸭肉", "鹅肉", "肉卷", "火腿", "香肠", "meat"});
        CATEGORY_KEYWORDS.put("蛋类", new String[]{"蛋", "鸡蛋", "鸭蛋", "鹅蛋", "鹌鹑蛋", "皮蛋", "松花蛋", "柴鸡蛋", "土鸡蛋", "egg"});
        CATEGORY_KEYWORDS.put("粮油", new String[]{"粮", "油", "米", "面", "大米", "小米", "面粉", "花生油", "菜籽油", "黑米", "红豆", "玉米", "绿豆", "黄豆", "糙米", "grain", "oil"});
        CATEGORY_KEYWORDS.put("干货", new String[]{"干货", "香菇", "木耳", "黑木耳", "红枣", "枸杞", "桂圆", "银耳", "莲子", "百合", "黄花菜", "干贝", "虾米", "dried"});
        CATEGORY_KEYWORDS.put("坚果", new String[]{"坚果", "核桃", "开心果", "夏威夷果", "碧根果", "腰果", "松子", "杏仁", "花生", "瓜子", "榛子", "nut"});
        CATEGORY_KEYWORDS.put("水产", new String[]{"水产", "海鲜", "鱼", "虾", "蟹", "大闸蟹", "带鱼", "鲈鱼", "基围虾", "紫菜", "海带", "海参", "鲍鱼", "seafood", "fish"});
        CATEGORY_KEYWORDS.put("菌菇", new String[]{"菌", "菇", "蘑菇", "松茸", "竹荪", "金针菇", "杏鲍菇", "茶树菇", "平菇", "香菇", "mushroom"});
        CATEGORY_KEYWORDS.put("蜂蜜", new String[]{"蜂蜜", "蜜", "蜂王浆", "百花蜜", "槐花蜜", "枣花蜜", "honey"});
        CATEGORY_KEYWORDS.put("茶叶", new String[]{"茶", "茶叶", "龙井", "普洱", "铁观音", "碧螺春", "白茶", "花茶", "茉莉花茶", "红茶", "绿茶", "tea"});
    }

    private List<Product> filterProductsByQuery(List<Product> allProducts, String query) {
        if (allProducts == null || allProducts.isEmpty()) return allProducts;
        if (query == null || query.trim().isEmpty()) return allProducts;

        String lowerQuery = query.toLowerCase();
        Set<Long> matchedCategoryIds = new HashSet<>();
        List<String> matchedCategoryNames = new ArrayList<>();

        for (Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            String categoryName = entry.getKey();
            String[] keywords = entry.getValue();
            for (String kw : keywords) {
                if (lowerQuery.contains(kw.toLowerCase())) {
                    matchedCategoryNames.add(categoryName);
                    break;
                }
            }
        }

        logger.info("[AI过滤] 查询'{}'匹配到分类名: {}", query, matchedCategoryNames);

        if (matchedCategoryNames.isEmpty()) return allProducts;

        try {
            List<Category> categories = categoryService.listAll();
            if (categories != null) {
                for (Category cat : categories) {
                    for (String catName : matchedCategoryNames) {
                        if (cat.getName() != null && (cat.getName().equals(catName) || cat.getName().contains(catName))) {
                            matchedCategoryIds.add(cat.getId());
                        }
                    }
                }
            }
            logger.info("[AI过滤] 匹配到分类ID: {}", matchedCategoryIds);
        } catch (Exception e) {
            logger.warn("查询分类失败: {}", e.getMessage());
        }

        if (matchedCategoryIds.isEmpty()) return allProducts;

        List<Product> filtered = new ArrayList<>();
        for (Product product : allProducts) {
            if (product.getCategoryId() != null && matchedCategoryIds.contains(product.getCategoryId())) {
                filtered.add(product);
            }
        }

        if (filtered.isEmpty()) return allProducts;

        logger.info("[AI过滤] 过滤后商品数: {}", filtered.size());
        return filtered;
    }

    /**
     * 处理 AI 查询（非流式）
     * <p>
     * 核心流程：
     * <ol>
     *   <li>校验参数（serviceType 限定 1-3）</li>
     *   <li>从数据库拉取商品列表</li>
     *   <li>用关键词过滤出与查询相关的商品（构造上下文）</li>
     *   <li>构造 Prompt → 调 DeepSeek API</li>
     *   <li>解析 AI 响应</li>
     *   <li>**兜底逻辑**：当 AI 结果不足时按销量补充推荐商品</li>
     *   <li>写 ai_service_log 日志</li>
     * </ol>
     * </p>
     *
     * @param userId      用户 ID
     * @param query       用户提问内容（可空，默认"推荐一些农产品"）
     * @param serviceType 服务类型：1-商品咨询 2-订单咨询 3-其他（可空，默认 1）
     * @return 包含 AI 回答、推荐商品、调试信息的 Map
     */
    public Map<String, Object> handleQuery(Long userId, String query, Integer serviceType) {
        long startTime = System.currentTimeMillis();
        try {
            if (query == null || query.trim().isEmpty()) {
                query = "推荐一些农产品";
            }

            // ===== 问题7：意图识别与动态路由 =====
            IntentClassifierService.ClassificationResult intentResult = null;
            if (intentClassifierService != null) {
                try {
                    intentResult = intentClassifierService.classify(query);
                    // 意图识别覆盖 serviceType（仅当未显式指定时）
                    if (serviceType == null) {
                        serviceType = intentClassifierService.getServiceType(intentResult.intent);
                    }
                    // 记录意图到监控
                    if (ragMonitorService != null) {
                        ragMonitorService.recordIntent(intentResult.intent.name());
                        ragMonitorService.recordQuery(query);
                    }
                    logger.info("[意图识别] query='{}', intent={}, confidence={}",
                            query, intentResult.intent.label,
                            String.format("%.4f", intentResult.confidence));
                } catch (Exception e) {
                    logger.warn("意图识别失败，使用默认 serviceType: {}", e.getMessage());
                }
            }

            if (serviceType == null || serviceType < 1 || serviceType > 3) {
                serviceType = 1;
            }

            // ===== 问题3：敏感信息过滤（对用户查询） =====
            String filteredQuery = query;
            if (contentFilterService != null) {
                try {
                    filteredQuery = contentFilterService.filterUserQuery(query);
                    if (!filteredQuery.equals(query)) {
                        logger.info("[内容过滤] 用户查询包含敏感信息，已过滤");
                    }
                } catch (Exception e) {
                    logger.warn("查询过滤失败，使用原始查询: {}", e.getMessage());
                }
            }

            logger.debug("Original query: '{}', filtered: '{}'", query, filteredQuery);

            List<Product> allProducts = null;
            try {
                allProducts = productService.listAll();
            } catch (Exception e) {
                logger.warn("获取商品列表失败: {}", e.getMessage());
                allProducts = new ArrayList<>();
            }
            logger.debug("Total products from database: {}", allProducts != null ? allProducts.size() : 0);

            List<Product> limitedProducts = filterProductsByQuery(allProducts, filteredQuery);

            logger.debug("Products for context: {}", limitedProducts != null ? limitedProducts.size() : 0);

            // ===== 问题6：商品上下文优化（相关性排序 + Token 预算控制） =====
            String productContext;
            if (productContextOptimizer != null) {
                try {
                    List<Product> ranked = productContextOptimizer.rankAndSelect(limitedProducts, filteredQuery);
                    productContext = productContextOptimizer.buildOptimizedContext(ranked);
                    // ===== 问题3：对商品上下文进行敏感信息过滤 =====
                    if (contentFilterService != null) {
                        productContext = contentFilterService.filterProductContext(productContext);
                    }
                } catch (Exception e) {
                    logger.warn("商品上下文优化失败，降级为原始方法: {}", e.getMessage());
                    productContext = buildProductContext(limitedProducts);
                }
            } else {
                productContext = buildProductContext(limitedProducts);
            }
            logger.debug("Product context length: {}", productContext.length());

            String response = null;
            long llmStart = System.currentTimeMillis();
            boolean llmSuccess = false;
            try {
                logger.debug("尝试使用DeepSeek API生成回复...");
                response = callDeepSeekAPI(filteredQuery, serviceType, productContext);
                llmSuccess = true;
                logger.debug("DeepSeek API调用成功，生成回复: {}...", response.substring(0, Math.min(100, response.length())));
            } catch (Exception e) {
                logger.warn("DeepSeek API调用失败: {}", e.getMessage());
                try {
                    logger.debug("尝试使用本地模拟回复...");
                    response = generateAIResponseWithUser(filteredQuery, serviceType, userId);
                    llmSuccess = true;
                    logger.debug("本地回复生成成功: {}", response);
                } catch (Exception ex) {
                    logger.warn("本地回复生成也失败: {}", ex.getMessage());
                    response = defaultResponse(filteredQuery);
                    logger.debug("使用默认回复: {}", response);
                }
            } finally {
                // ===== 问题5：监控埋点 =====
                if (ragMonitorService != null) {
                    long llmDuration = System.currentTimeMillis() - llmStart;
                    ragMonitorService.recordLlmCall(llmDuration, llmSuccess);
                }
            }

            // 将新的查询结果存入缓存
            if (response != null) {
                queryCache.put(query, response);

                // 控制缓存大小，避免内存溢出
                if (queryCache.size() > 100) {
                    List<String> keys = new ArrayList<>(queryCache.keySet());
                    for (int i = 0; i < 50 && i < keys.size(); i++) {
                        queryCache.remove(keys.get(i));
                    }
                }
            } else {
                response = defaultResponse(filteredQuery);
            }

            // 记录服务日志
            AIServiceLog log = new AIServiceLog();
            log.setUserId(userId != null && userId != 0L ? userId : null);
            log.setQuery(query);
            log.setResponse(response);
            log.setServiceType(serviceType);
            log.setCreatedAt(LocalDateTime.now());
            aiServiceLogMapper.insert(log);

            Map<String, Object> result = new HashMap<>();
            result.put("response", response);
            result.put("logId", log.getId());
            result.put("responseTimeMs", System.currentTimeMillis() - startTime);
            if (intentResult != null) {
                result.put("intent", intentResult.intent.name());
                result.put("intentLabel", intentResult.intent.label);
                result.put("intentConfidence", Math.round(intentResult.confidence * 10000) / 10000.0);
            }
            return result;
        } catch (Exception e) {
            logger.error("处理AI查询失败: {}", e.getMessage(), e);
            AIServiceLog log = new AIServiceLog();
            log.setUserId(userId != null && userId != 0L ? userId : null);
            log.setQuery(query);
            log.setResponse("抱歉，暂时无法回答您的问题，请稍后重试");
            log.setServiceType(serviceType);
            log.setCreatedAt(LocalDateTime.now());
            try {
                aiServiceLogMapper.insert(log);
            } catch (Exception ex) {
                logger.error("记录服务日志失败: {}", ex.getMessage(), ex);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("response", "抱歉，暂时无法回答您的问题，请稍后重试");
            result.put("logId", log.getId());
            result.put("responseTimeMs", System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * 处理 AI 查询（SSE 流式输出）
     * <p>
     * 与 handleQuery 流程一致，但通过 SseEmitter 实时推送 AI 生成的 token，
     * 用户端可看到"打字机效果"。超时时间 120 秒。
     * </p>
     *
     * @param userId      用户 ID
     * @param query       用户提问
     * @param serviceType 服务类型
     * @return SseEmitter 流式响应器
     */
    public SseEmitter handleQueryStream(Long userId, String query, Integer serviceType) {
        SseEmitter emitter = new SseEmitter(120000L);

        if (serviceType == null || serviceType < 1 || serviceType > 3) {
            serviceType = 1;
        }
        if (query == null || query.trim().isEmpty()) {
            query = "推荐一些农产品";
        }

        final Integer finalServiceType = serviceType;
        final String finalQuery = query;

        executor.execute(() -> {
            logger.info("[AI异步] 异步任务开始执行, query={}, serviceType={}", finalQuery, finalServiceType);
            StringBuilder fullResponse = new StringBuilder();
            try {
                List<Product> allProducts = productService.listAll();
                if (allProducts == null) allProducts = new ArrayList<>();

                List<Product> limitedProducts = filterProductsByQuery(allProducts, finalQuery);

                String productContext = buildProductContext(limitedProducts);
                String systemPrompt = buildSystemPrompt(finalServiceType, productContext);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", deepSeekConfig.getModel());
                requestBody.put("temperature", deepSeekConfig.getTemperature());
                requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());
                requestBody.put("stream", true);

                Map<String, String> thinking = new HashMap<>();
                thinking.put("type", "disabled");
                requestBody.put("thinking", thinking);

                List<Map<String, String>> messages = new ArrayList<>();
                Map<String, String> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);

                Map<String, String> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", finalQuery);
                messages.add(userMsg);

                requestBody.put("messages", messages);

                String requestJson = OBJECT_MAPPER.writeValueAsString(requestBody);
                logger.info("请求DeepSeek API: URL={}, model={}", deepSeekConfig.getApiUrl(), deepSeekConfig.getModel());

                org.apache.http.client.methods.HttpPost httpPost =
                    new org.apache.http.client.methods.HttpPost(deepSeekConfig.getApiUrl());
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Authorization", "Bearer " + deepSeekConfig.getApiKey());
                httpPost.setEntity(new org.apache.http.entity.StringEntity(requestJson, "UTF-8"));

                RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(10000)
                    .setSocketTimeout(60000)
                    .build();

                try (CloseableHttpClient httpClient = HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .build()) {

                    org.apache.http.HttpResponse response = httpClient.execute(httpPost);
                    int statusCode = response.getStatusLine().getStatusCode();
                    logger.info("DeepSeek API响应状态码: {}", statusCode);

                    if (statusCode != 200) {
                        StringBuilder errorBody = new StringBuilder();
                        try (BufferedReader errorReader = new BufferedReader(
                                new InputStreamReader(response.getEntity().getContent(), "UTF-8"))) {
                            String errorLine;
                            while ((errorLine = errorReader.readLine()) != null) {
                                errorBody.append(errorLine);
                            }
                        }
                        logger.error("DeepSeek API返回错误: status={}, body={}", statusCode, errorBody.toString());
                        throw new RuntimeException("DeepSeek API返回错误状态码: " + statusCode + ", 响应: " + errorBody.toString());
                    }

                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                            String jsonData = line.substring(6);
                            try {
                                JsonNode node = OBJECT_MAPPER.readTree(jsonData);
                                JsonNode choices = node.get("choices");
                                if (choices != null && choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).get("delta");
                                    if (delta != null) {
                                        JsonNode content = delta.get("content");
                                        if (content != null && content.isTextual()) {
                                            String token = content.asText();
                                            fullResponse.append(token);
                                            Map<String, String> event = new HashMap<>();
                                            event.put("token", token);
                                            emitter.send(SseEmitter.event()
                                                .name("token")
                                                .data(event));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.warn("解析SSE数据失败: {}", e.getMessage());
                            }
                        }
                    }
                    reader.close();
                }

                String cleanedResponse = fullResponse.toString()
                    .replaceAll("###+", " ")
                    .replaceAll("\\*\\*", "")
                    .replaceAll("__", "")
                    .replaceAll("(?m)^- ", "")
                    .replaceAll("(?m)^#+ ", "")
                    .replaceAll("\\n\\s+\\n", "\\n")
                    .trim();

                if (cleanedResponse.isEmpty()) {
                    cleanedResponse = generateAIResponseWithUser(finalQuery, finalServiceType, userId);
                }

                Map<String, Object> doneEvent = new HashMap<>();
                doneEvent.put("response", cleanedResponse);

                if (finalServiceType == 1) {
                    List<Map<String, Object>> productCards = buildProductCards(
                        getRelatedProducts(finalQuery, cleanedResponse), cleanedResponse);
                    if (!productCards.isEmpty()) {
                        doneEvent.put("productCards", productCards);
                        logger.info("[AI异步] 附带 {} 个商品卡片", productCards.size());
                    }
                }

                if (isDiscountQuery(finalQuery)) {
                    List<DiscountActivity> activities = getRelatedActivities();
                    if (activities != null && !activities.isEmpty()) {
                        doneEvent.put("activityCards", buildActivityCards(activities));
                        logger.info("[AI异步] 附带 {} 个优惠活动卡片", activities.size());
                    }
                }

                emitter.send(SseEmitter.event()
                    .name("done")
                    .data(doneEvent));

                AIServiceLog log = new AIServiceLog();
                log.setUserId(userId != null && userId != 0L ? userId : null);
                log.setQuery(finalQuery);
                log.setResponse(cleanedResponse);
                log.setServiceType(finalServiceType);
                log.setCreatedAt(LocalDateTime.now());
                aiServiceLogMapper.insert(log);

                emitter.complete();

            } catch (Exception e) {
                logger.error("DeepSeek API流式调用失败，使用本地回复", e);

                try {
                    String fallback = generateAIResponseWithUser(finalQuery, finalServiceType, userId);
                    if (fallback == null || fallback.isEmpty()) {
                        fallback = "很抱歉，我暂时无法处理您的请求。您可以尝试重新提问，或联系在线客服获取帮助。";
                    }
                    Map<String, Object> doneEvent = new HashMap<>();
                    doneEvent.put("response", fallback);

                    if (finalServiceType == 1) {
                        try {
                            List<Map<String, Object>> productCards = buildProductCards(
                                getRelatedProducts(finalQuery, fallback), fallback);
                            if (!productCards.isEmpty()) {
                                doneEvent.put("productCards", productCards);
                            }
                        } catch (Exception cardEx) {
                            logger.warn("构建商品卡片失败: {}", cardEx.getMessage());
                        }
                    }

                    try {
                        if (isDiscountQuery(finalQuery)) {
                            List<DiscountActivity> activities = getRelatedActivities();
                            if (activities != null && !activities.isEmpty()) {
                                doneEvent.put("activityCards", buildActivityCards(activities));
                            }
                        }
                    } catch (Exception activityEx) {
                        logger.warn("构建活动卡片失败: {}", activityEx.getMessage());
                    }

                    emitter.send(SseEmitter.event().name("done")
                        .data(doneEvent));

                    AIServiceLog log = new AIServiceLog();
                    log.setUserId(userId != null && userId != 0L ? userId : null);
                    log.setQuery(finalQuery);
                    log.setResponse(fallback);
                    log.setServiceType(finalServiceType);
                    log.setCreatedAt(LocalDateTime.now());
                    aiServiceLogMapper.insert(log);

                    emitter.complete();
                } catch (Exception ex) {
                    logger.error("AI降级处理完全失败: {}", ex.getMessage());
                    try { emitter.complete(); } catch (Exception ignored) {}
                }
            }
        });

        return emitter;
    }

    private List<Map<String, Object>> buildProductCards(List<Product> products, String aiResponse) {
        List<Map<String, Object>> cards = new ArrayList<>();
        if (products == null || products.isEmpty()) {
            return cards;
        }
        int limit = Math.min(products.size(), 10);
        for (int i = 0; i < limit; i++) {
            Product p = products.get(i);
            Map<String, Object> card = new HashMap<>();
            card.put("id", p.getId());
            card.put("name", p.getName());
            card.put("price", p.getPrice());
            card.put("image", p.getCover());
            card.put("description", truncateText(p.getDescription(), 60));
            card.put("reason", generateRecommendReason(p, aiResponse));
            card.put("sales", p.getSales() != null ? p.getSales() : 0);
            try {
                Category cat = categoryService.findById(p.getCategoryId());
                card.put("category", cat != null ? cat.getName() : "");
            } catch (Exception ignored) {
                card.put("category", "");
            }
            cards.add(card);
        }
        return cards;
    }

    private String truncateText(String text, int maxLen) {
        if (text == null || text.isEmpty()) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    private String generateRecommendReason(Product p, String aiResponse) {
        if (aiResponse != null && !aiResponse.isEmpty() && p.getName() != null) {
            int idx = aiResponse.indexOf(p.getName());
            if (idx >= 0) {
                int start = Math.max(0, idx - 20);
                int end = Math.min(aiResponse.length(), idx + p.getName().length() + 80);
                String snippet = aiResponse.substring(start, end).trim();
                snippet = snippet.replaceAll("[\\n\\r]+", " ");
                if (snippet.length() > 80) {
                    snippet = snippet.substring(0, 80) + "...";
                }
                if (snippet.contains(p.getName())) return snippet;
            }
        }
        if (p.getDescription() != null && !p.getDescription().isEmpty()) {
            return truncateText(p.getDescription(), 80);
        }
        if (p.getSales() != null && p.getSales() > 100) return "热销爆款，累计销量" + p.getSales() + "件，品质值得信赖";
        if (p.getSales() != null && p.getSales() > 50) return "销量稳步增长，深受用户喜爱的好产品";
        return "平台精选优质商品，产地直供，新鲜健康";
    }

    /**
     * 根据用户 query + AI 回答，智能匹配相关商品（用于在 AI 回复中插入商品卡片）
     *
     * @param query      用户提问
     * @param aiResponse AI 回答内容
     * @return 匹配的商品列表（最多 5 个）
     */
    public List<Product> getRelatedProducts(String query, String aiResponse) {
        logger.info("[商品卡片] 智能匹配商品, query={}", query);
        List<Product> allProducts = productService.listAll();
        if (allProducts == null || allProducts.isEmpty()) {
            logger.warn("[商品卡片] 数据库中没有商品");
            return new ArrayList<>();
        }

        List<Product> result = new ArrayList<>();
        Set<Long> addedIds = new HashSet<>();
        final int TARGET = 10;

        // 策略1: 从AI回复中提取商品ID
        if (aiResponse != null && !aiResponse.isEmpty()) {
            java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile("ID[:：](\\d+)");
            java.util.regex.Matcher idMatcher = idPattern.matcher(aiResponse);
            while (idMatcher.find() && result.size() < TARGET) {
                try {
                    Long id = Long.parseLong(idMatcher.group(1));
                    if (addedIds.add(id)) {
                        Product p = productService.findById(id);
                        if (p != null) {
                            result.add(p);
                            logger.info("[商品卡片] AI回复ID匹配: {}", p.getName());
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // 策略2: 数据库关键词搜索
        if (result.size() < TARGET && query != null && !query.isEmpty()) {
            String searchKeyword = extractCoreSearchTerm(query);
            if (searchKeyword != null && !searchKeyword.isEmpty()) {
                List<Product> searched = productService.search(searchKeyword);
                if (searched != null) {
                    for (Product p : searched) {
                        if (addedIds.add(p.getId())) {
                            result.add(p);
                            logger.info("[商品卡片] 搜索匹配: {}", p.getName());
                            if (result.size() >= TARGET) break;
                        }
                    }
                }
            }
        }

        // 策略3: AI回复中直接提到的商品名称
        if (result.size() < TARGET && aiResponse != null && !aiResponse.isEmpty()) {
            for (Product p : allProducts) {
                if (addedIds.contains(p.getId())) continue;
                if (p.getName() != null && p.getName().length() >= 2 && aiResponse.contains(p.getName())) {
                    result.add(p);
                    addedIds.add(p.getId());
                    logger.info("[商品卡片] AI回复名称匹配: {}", p.getName());
                    if (result.size() >= TARGET) break;
                }
            }
        }

        // 策略4: 关键词与商品名/描述的双向模糊匹配
        if (result.size() < TARGET && query != null && !query.isEmpty()) {
            List<String> kws = extractKeywordsForMatching(query);
            for (Product p : allProducts) {
                if (addedIds.contains(p.getId())) continue;
                String name = p.getName() != null ? p.getName() : "";
                String desc = p.getDescription() != null ? p.getDescription() : "";
                for (String kw : kws) {
                    if (kw.isEmpty()) continue;
                    if (isFuzzyMatch(name, kw) || isFuzzyMatch(desc, kw)) {
                        result.add(p);
                        addedIds.add(p.getId());
                        logger.info("[商品卡片] 模糊匹配: {} <=> {}", p.getName(), kw);
                        break;
                    }
                }
                if (result.size() >= TARGET) break;
            }
        }

        // 策略5: 分类扩展匹配 - 如果匹配到的商品属于某些分类，补充同分类下的其他商品
        if (result.size() < TARGET && !result.isEmpty()) {
            Set<Long> matchedCategoryIds = new HashSet<>();
            for (Product p : result) {
                if (p.getCategoryId() != null) matchedCategoryIds.add(p.getCategoryId());
            }
            for (Product p : allProducts) {
                if (addedIds.contains(p.getId())) continue;
                if (p.getCategoryId() != null && matchedCategoryIds.contains(p.getCategoryId())) {
                    result.add(p);
                    addedIds.add(p.getId());
                    logger.info("[商品卡片] 同分类补充: {}", p.getName());
                    if (result.size() >= TARGET) break;
                }
            }
        }

        // 策略6: 确保多样性 - 少量补充高销量商品（已有匹配时最多补3个）
        if (result.size() < TARGET) {
            int hotFillLimit = result.isEmpty() ? TARGET : Math.min(result.size() + 3, TARGET);
            List<Product> sorted = new ArrayList<>(allProducts);
            sorted.sort((a, b) -> Integer.compare(
                b.getSales() != null ? b.getSales() : 0,
                a.getSales() != null ? a.getSales() : 0));
            for (Product p : sorted) {
                if (addedIds.add(p.getId())) {
                    result.add(p);
                    logger.info("[商品卡片] 热销补充: {}", p.getName());
                    if (result.size() >= hotFillLimit) break;
                }
            }
        }

        // 最终fallback
        if (result.isEmpty()) {
            int limit = Math.min(allProducts.size(), TARGET);
            for (int i = 0; i < limit; i++) {
                result.add(allProducts.get(i));
            }
            logger.info("[商品卡片] 使用fallback前{}个商品", limit);
        }

        logger.info("[商品卡片] 最终返回{}个商品: {}", result.size(),
            result.stream().map(Product::getName).reduce((a, b) -> a + ", " + b).orElse("无"));
        return result;
    }

    // 从查询中提取核心搜索词
    private String extractCoreSearchTerm(String query) {
        if (query == null || query.isEmpty()) return query;
        String cleaned = query.trim()
                .replaceAll("[\\p{Punct}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() <= 3) return cleaned;
        return cleaned.substring(0, Math.min(8, cleaned.length()));
    }

    // 从查询中提取用于匹配的关键词
    private List<String> extractKeywordsForMatching(String query) {
        List<String> kws = new ArrayList<>();
        if (query == null || query.isEmpty()) return kws;

        String normalized = query.trim()
                .replaceAll("[\\p{Punct}]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        for (Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            String[] keywords = entry.getValue();
            for (String kw : keywords) {
                if (normalized.contains(kw)) {
                    kws.add(entry.getKey());
                    break;
                }
            }
        }

        String[] extraKeywords = {
            "有机", "绿色", "天然", "生态", "新鲜", "健康", "特产", "礼盒",
            "便宜", "实惠", "性价比", "推荐", "热门", "爆款", "新品", "进口"
        };

        for (String kw : extraKeywords) {
            if (normalized.contains(kw)) {
                kws.add(kw);
            }
        }

        if (kws.isEmpty()) {
            String[] words = normalized.split(" ");
            for (String word : words) {
                if (word.length() > 1 && !isStopWord(word)) {
                    kws.add(word);
                }
            }
        }

        if (kws.isEmpty()) {
            kws.add(normalized);
        }

        return kws;
    }

    // 双向模糊匹配：检查目标字符串和关键词之间是否存在包含关系或单字重叠
    private boolean isFuzzyMatch(String target, String keyword) {
        if (target == null || keyword == null || target.isEmpty() || keyword.isEmpty()) return false;
        // 包含匹配（双向）
        if (target.contains(keyword) || keyword.contains(target)) return true;
        // 单字匹配：关键词较短时，检查每个字是否都在目标中出现
        if (keyword.length() <= 3) {
            for (int i = 0; i < keyword.length(); i++) {
                if (target.indexOf(keyword.charAt(i)) < 0) return false;
            }
            return true;
        }
        return false;
    }
    
    // 检测是否是优惠活动相关查询
    private boolean isDiscountQuery(String query) {
        if (query == null || query.isEmpty()) return false;
        String[] discountKeywords = {
            "优惠", "活动", "折扣", "促销", "满减", "秒杀", "限时",
            "打折", "特价", "优惠券", "红包", "福利", "活动价",
            "有什么活动", "最近活动", "现在活动", "当前活动"
        };
        for (String kw : discountKeywords) {
            if (query.contains(kw)) return true;
        }
        return false;
    }

    // 获取当前可用的优惠活动
    private List<DiscountActivity> getRelatedActivities() {
        try {
            return discountActivityService.getActiveActivities();
        } catch (Exception e) {
            logger.warn("获取优惠活动失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // 构建优惠活动上下文信息（给AI系统提示词用）
    private String buildActivityContext() {
        List<DiscountActivity> activities = getRelatedActivities();
        if (activities == null || activities.isEmpty()) {
            return "当前暂无优惠活动";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < activities.size(); i++) {
            DiscountActivity a = activities.get(i);
            sb.append((i + 1)).append(". ").append(a.getName());
            if (a.getType() != null) {
                String typeName = "";
                switch (a.getType()) {
                    case 1: typeName = "满减"; break;
                    case 2: typeName = "限时折扣"; break;
                    case 3: typeName = "秒杀"; break;
                    default: typeName = "折扣";
                }
                sb.append("（").append(typeName).append("）");
            }
            if (a.getThreshold() != null && a.getThreshold().compareTo(java.math.BigDecimal.ZERO) > 0 && a.getReduceAmount() != null && a.getReduceAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                sb.append("：满").append(String.format("%.0f", a.getThreshold())).append("减").append(String.format("%.0f", a.getReduceAmount()));
            } else if (a.getDiscountRate() != null && a.getDiscountRate().compareTo(java.math.BigDecimal.ZERO) > 0 && a.getDiscountRate().compareTo(java.math.BigDecimal.TEN) < 0) {
                sb.append("：").append(String.format("%.1f", a.getDiscountRate())).append("折");
            }
            if (a.getDescription() != null && !a.getDescription().isEmpty()) {
                sb.append("，").append(a.getDescription());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // 构建优惠活动卡片数据
    private List<Map<String, Object>> buildActivityCards(List<DiscountActivity> activities) {
        List<Map<String, Object>> cards = new ArrayList<>();
        if (activities == null || activities.isEmpty()) return cards;
        int limit = Math.min(activities.size(), 5);
        for (int i = 0; i < limit; i++) {
            DiscountActivity a = activities.get(i);
            Map<String, Object> card = new HashMap<>();
            card.put("id", a.getId());
            card.put("name", a.getName());
            card.put("type", a.getType());
            String typeName = "";
            switch (a.getType() != null ? a.getType() : 0) {
                case 1: typeName = "满减"; break;
                case 2: typeName = "限时折扣"; break;
                case 3: typeName = "秒杀"; break;
                default: typeName = "活动";
            }
            card.put("typeName", typeName);
            card.put("discountRate", a.getDiscountRate());
            card.put("threshold", a.getThreshold());
            card.put("reduceAmount", a.getReduceAmount());
            card.put("description", a.getDescription());
            card.put("startTime", a.getStartTime() != null ? a.getStartTime().toString() : null);
            card.put("endTime", a.getEndTime() != null ? a.getEndTime().toString() : null);
            cards.add(card);
        }
        return cards;
    }

    // 判断是否为停用词
    private boolean isStopWord(String word) {
        String[] stopWords = {
            "的", "了", "是", "在", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看",
            "好", "自己", "这", "我们", "起", "来", "那么", "里", "那", "个", "地", "得",
            "什么", "你们", "他们", "她们", "它们", "能", "可以", "应该", "需要",
            "推荐", "介绍", "查询", "了解", "购买", "价格", "库存", "描述",
            "特点", "优势", "营养", "功效", "用法", "保存", "产地", "品牌"
        };
        
        for (String sw : stopWords) {
            if (sw.equals(word)) {
                return true;
            }
        }
        return false;
    }

    // 构建商品上下文
    private String buildProductContext(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "当前没有相关商品信息。";
        }

        Map<String, List<Product>> grouped = new LinkedHashMap<>();
        for (Product product : products) {
            String categoryName = "未知分类";
            try {
                Category category = categoryService.findById(product.getCategoryId());
                if (category != null) categoryName = category.getName();
            } catch (Exception e) {
                logger.warn("获取分类失败: {}", e.getMessage());
            }
            grouped.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(product);
        }

        StringBuilder context = new StringBuilder();
        context.append("平台商品分类概览（共").append(products.size()).append("个商品，").append(grouped.size()).append("个分类）：\n");
        for (Map.Entry<String, List<Product>> entry : grouped.entrySet()) {
            context.append("- ").append(entry.getKey()).append("(").append(entry.getValue().size()).append("个商品)\n");
        }
        context.append("\n商品详细信息：\n\n");

        for (Product product : products) {
            String categoryName = "未知分类";
            try {
                Category category = categoryService.findById(product.getCategoryId());
                if (category != null) categoryName = category.getName();
            } catch (Exception e) {}

            context.append("ID:").append(product.getId())
                    .append(" | ").append(product.getName())
                    .append(" | 分类:").append(categoryName)
                    .append(" | ¥").append(product.getPrice())
                    .append(" | 库存:").append(product.getStock());
            if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                String desc = product.getDescription().length() > 80 
                    ? product.getDescription().substring(0, 80) + "..." 
                    : product.getDescription();
                context.append(" | ").append(desc);
            }
            context.append("\n");
        }
        return context.toString();
    }
    
    // 构建系统提示词
    private String buildSystemPrompt(Integer serviceType, String productContext) {
        switch (serviceType) {
            case 1:
                return "你是「乡村振兴」农产品电商平台的AI购物助手，根据用户需求推荐平台上的真实商品。\n\n" +
                       "【平台商品数据】\n" + productContext + "\n" +
                       "【当前优惠活动】\n" + buildActivityContext() + "\n" +
                       "【回答规则】\n" +
                       "1. 必须基于上方商品数据推荐，不得编造不存在的商品\n" +
                       "2. 推荐商品时必须附带商品ID，格式：商品名(ID:数字)，如 芒果(ID:5341)\n" +
                       "3. 根据用户需求精准匹配商品类型，用户问水果就只推荐水果分类下的商品\n" +
                       "4. 同类商品尽量多推荐几个供用户选择\n" +
                       "5. 简要说明推荐理由（口感、产地、营养等）\n" +
                       "6. 用户问优惠活动时，详细介绍活动规则并推荐参与活动的商品\n" +
                       "7. 用自然口语化语言回答，不要使用Markdown格式、列表编号或特殊符号\n" +
                       "8. 不要使用开场白和结束语，直接回答问题";
            case 2:
                return "你是「乡村振兴」农产品电商平台的物流查询助手。\n\n" +
                       "【回答规则】\n" +
                       "1. 根据用户的订单信息查询物流状态\n" +
                       "2. 如有物流单号，告知承运商和运单号\n" +
                       "3. 如用户未登录或无订单，引导用户登录后查看\n" +
                       "4. 用自然口语化语言回答，不要使用Markdown格式\n" +
                       "5. 不要使用开场白和结束语，直接回答问题";
            case 3:
                return "你是「乡村振兴」农产品电商平台的售后咨询助手。\n\n" +
                       "【售后政策】\n" +
                       "- 签收7天内可无理由退换货\n" +
                       "- 质量问题随时可退换，运费卖家承担\n" +
                       "- 退款1-3个工作日到账\n\n" +
                       "【回答规则】\n" +
                       "1. 根据用户售后记录提供准确信息\n" +
                       "2. 引导用户通过个人中心提交售后申请\n" +
                       "3. 用自然口语化语言回答，不要使用Markdown格式\n" +
                       "4. 不要使用开场白和结束语，直接回答问题";
            default:
                return "你是「乡村振兴」农产品电商平台的AI助手。\n\n" +
                       "【平台商品数据】\n" + productContext + "\n" +
                       "【回答规则】\n" +
                       "1. 基于真实商品数据回答，不得编造商品\n" +
                       "2. 推荐商品时附带商品ID，格式：商品名(ID:数字)\n" +
                       "3. 用自然口语化语言回答，不要使用Markdown格式\n" +
                       "4. 不要使用开场白和结束语，直接回答问题";
        }
    }
    
    // 调用DeepSeek API获取AI回复
    private String callDeepSeekAPI(String query, Integer serviceType, String productContext) throws Exception {
        String systemPrompt = buildSystemPrompt(serviceType, productContext);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepSeekConfig.getModel());
        requestBody.put("temperature", deepSeekConfig.getTemperature());
        requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());
        
        List<Map<String, Object>> messages = new ArrayList<>();
        
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", query);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekConfig.getApiKey());
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                deepSeekConfig.getApiUrl(), requestEntity, String.class);
        
        ObjectMapper mapper = OBJECT_MAPPER;
        JsonNode rootNode = mapper.readTree(responseEntity.getBody());
        JsonNode choicesNode = rootNode.get("choices");
        if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
            JsonNode messageNode = choicesNode.get(0).get("message");
            if (messageNode != null) {
                JsonNode contentNode = messageNode.get("content");
                if (contentNode != null && contentNode.isTextual()) {
                    String rawResponse = contentNode.asText();
                    String cleanedResponse = rawResponse
                            .replaceAll("###+", " ")
                            .replaceAll("\\*\\*", "")
                            .replaceAll("__", "")
                            .replaceAll("(?m)^- ", "")
                            .replaceAll("(?m)^#+ ", "")
                            .replaceAll("\\n\\s+\\n", "\\n")
                            .trim();
                    return cleanedResponse;
                }
            }
        }
        
        return defaultResponse(query);
    }

    // 带用户ID的本地回复生成
    private String generateAIResponseWithUser(String query, Integer serviceType, Long userId) {
        try {
            String lowerQuery = query.toLowerCase();
            
            switch (serviceType) {
                case 1:
                    return handleProductQuery(lowerQuery, query);
                case 2:
                    return handleLogisticsQuery(lowerQuery, query, userId);
                case 3:
                    return handleAfterSaleQuery(lowerQuery, query, userId);
                default:
                    if (lowerQuery.contains("商品") || lowerQuery.contains("农产品") || lowerQuery.contains("购买") || lowerQuery.contains("推荐")) {
                        return handleProductQuery(lowerQuery, query);
                    } else if (lowerQuery.contains("物流") || lowerQuery.contains("快递") || lowerQuery.contains("配送") || lowerQuery.contains("跟踪")) {
                        return handleLogisticsQuery(lowerQuery, query, userId);
                    } else if (lowerQuery.contains("售后") || lowerQuery.contains("退款") || lowerQuery.contains("退换") || lowerQuery.contains("投诉")) {
                        return handleAfterSaleQuery(lowerQuery, query, userId);
                    } else if (lowerQuery.contains("活动") || lowerQuery.contains("助农") || lowerQuery.contains("优惠") || lowerQuery.contains("促销")) {
                        return handleActivityQuery(lowerQuery, query);
                    } else if (lowerQuery.contains("种植") || lowerQuery.contains("技术") || lowerQuery.contains("农业") || lowerQuery.contains("养殖")) {
                        return handlePlantingTechQuery(lowerQuery, query);
                    } else if (lowerQuery.contains("价格") || lowerQuery.contains("行情") || lowerQuery.contains("市场") || lowerQuery.contains("趋势")) {
                        return handlePriceQuery(lowerQuery, query);
                    } else if (lowerQuery.contains("订单") || lowerQuery.contains("购买") || lowerQuery.contains("下单") || lowerQuery.contains("支付")) {
                        return handleOrderQuery(lowerQuery, query);
                    } else if (lowerQuery.contains("账号") || lowerQuery.contains("注册") || lowerQuery.contains("登录") || lowerQuery.contains("绑定")) {
                        return handleAccountQuery(lowerQuery, query);
                    } else {
                        return defaultResponse(query);
                    }
            }
        } catch (Exception e) {
            logger.warn("生成AI回复失败，使用默认回复: {}", e.getMessage());
            return defaultResponse(query);
        }
    }

    private String handleProductQuery(String lowerQuery, String originalQuery) {
        List<Product> allProducts = null;
        try {
            allProducts = productService.listAll();
        } catch (Exception e) {
            allProducts = new ArrayList<>();
        }
        if (allProducts == null) allProducts = new ArrayList<>();

        List<Product> matched = filterProductsByQuery(allProducts, originalQuery);
        if (matched.isEmpty()) matched = allProducts;

        int limit = Math.min(matched.size(), 5);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            Product p = matched.get(i);
            String catName = "未知分类";
            try {
                Category cat = categoryService.findById(p.getCategoryId());
                if (cat != null) catName = cat.getName();
            } catch (Exception e) {}
            if (i > 0) sb.append("；");
            sb.append(p.getName()).append("(ID:").append(p.getId()).append(")")
              .append("，分类：").append(catName)
              .append("，价格¥").append(p.getPrice());
            if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                String desc = p.getDescription().length() > 30 ? p.getDescription().substring(0, 30) + "..." : p.getDescription();
                sb.append("，").append(desc);
            }
        }
        return "为您推荐以下商品：" + sb.toString() + "。您可以点击商品卡片查看详情或直接购买。";
    }
    
    // 处理物流查询（支持真实数据查询）
    private String handleLogisticsQuery(String lowerQuery, String originalQuery, Long userId) {
        if (userId != null && userId > 0L) {
            try {
                List<Orders> userOrders = orderService.findByUserId(userId);
                if (userOrders != null && !userOrders.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("为您查询到以下订单的物流信息：\n\n");
                    int count = 0;
                    for (Orders order : userOrders) {
                        if (count >= 5) break;
                        String statusText = getOrderStatusText(order.getStatus());
                        sb.append("订单").append(order.getOrderSn()).append("（").append(statusText).append("）\n");
                        if (order.getLogisticsId() != null) {
                            Logistics logistics = logisticsService.findById(order.getLogisticsId());
                            if (logistics != null) {
                                sb.append("承运商：").append(logistics.getLogisticsCompany() != null ? logistics.getLogisticsCompany() : "待分配").append("\n");
                                sb.append("运单号：").append(logistics.getLogisticsNo() != null ? logistics.getLogisticsNo() : "待分配").append("\n");
                                List<LogisticsTrace> traces = logisticsService.findTraceByLogisticsId(logistics.getId());
                                if (traces != null && !traces.isEmpty()) {
                                    LogisticsTrace latest = traces.get(0);
                                    sb.append("最新状态：").append(latest.getDescription() != null ? latest.getDescription() : statusText).append("\n");
                                } else {
                                    sb.append("最新状态：").append(statusText).append("\n");
                                }
                            }
                        } else {
                            sb.append("暂无物流信息，请等待商家发货\n");
                        }
                        sb.append("\n");
                        count++;
                    }
                    sb.append("您可以登录账户进入订单详情查看更详细的信息，或联系在线客服。");
                    return sb.toString();
                }
            } catch (Exception e) {
                logger.warn("查询物流信息失败: {}", e.getMessage());
            }
        }
        // 没有登录或没有订单时的通用回复
        return "关于您的物流查询，麻烦您先登录账户，这样我就能为您查询具体的物流状态了。您也可以登录后进入订单详情页面查看物流信息。如果您已经登录，请提供订单号，我可以帮您查询。我们的物流合作伙伴是顺丰速运、京东物流等知名快递公司，会确保您的商品快速安全送达。";
    }
    
    // 处理售后咨询（支持真实数据查询）
    private String handleAfterSaleQuery(String lowerQuery, String originalQuery, Long userId) {
        if (userId != null && userId > 0L) {
            try {
                List<AfterSaleService> afterSales = afterSaleServiceApi.getAfterSalesByUserId(userId);
                if (afterSales != null && !afterSales.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("为您查询到以下售后服务记录：\n\n");
                    int count = 0;
                    for (AfterSaleService as : afterSales) {
                        if (count >= 5) break;
                        sb.append("售后类型：").append(getServiceTypeText(as.getServiceType())).append("，");
                    sb.append("状态：").append(getServiceStatusText(as.getStatus())).append("，");
                        sb.append("原因：").append(as.getReason()).append("，");
                        if (as.getRefundAmount() != null) {
                            sb.append("退款金额：").append(as.getRefundAmount()).append("元，");
                        }
                        if (as.getReturnLogistics() != null) {
                            sb.append("退货物流：").append(as.getReturnLogisticsCompany() != null ? as.getReturnLogisticsCompany() : "").append(" ").append(as.getReturnLogistics()).append("，");
                        }
                        sb.append("处理结果：").append(as.getServiceResult() != null ? as.getServiceResult() : "暂无");
                        sb.append("\n");
                        count++;
                    }
                    sb.append("您可以进入个人中心的售后服务页面查看详情或提交新的售后申请。");
                    return sb.toString();
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] 查询售后信息失败: " + e.getMessage());
            }
        }
        // 没有登录或没有售后记录时的通用回复
        return "针对您的售后问题，我们的售后服务政策是：商品签收后7天内可以无理由退换货，质量问题随时可退换，运费由卖家承担。申请流程很简单，登录账户后进入售后服务页面提交申请即可。如果您还没有登录，请先登录账户查询您的售后记录。您也可以直接联系客服热线400-123-4567。";
    }

    private String getServiceTypeText(Integer serviceType) {
        if (serviceType == null) return "未知";
        switch (serviceType) {
            case 1: return "退货退款";
            case 2: return "换货";
            case 3: return "维修";
            default: return "其他";
        }
    }

    private String getServiceStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待处理";
            case 1: return "处理中";
            case 2: return "已完成";
            case 3: return "已关闭";
            default: return "其他";
        }
    }

    private String getOrderStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待付款";
            case 1: return "已付款待发货";
            case 2: return "已发货";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "其他";
        }
    }
    
    // 处理活动咨询
    private String handleActivityQuery(String lowerQuery, String originalQuery) {
        return "关于您查询的助农活动，我们定期组织多种活动。每周五晚8点会有助农直播带货，邀请乡村主播介绍优质农产品。周末还会组织城市居民到乡村体验采摘乐趣。每年春秋两季我们会举办大型农产品展销会。另外，我们还与贫困地区合作开展助农扶贫项目，推广当地特色农产品。您可以关注平台公告或公众号，获取最新的活动信息。";
    }
    
    // 处理种植技术查询
    private String handlePlantingTechQuery(String lowerQuery, String originalQuery) {
        if (lowerQuery.contains("蔬菜") || lowerQuery.contains("种植")) {
            return "关于您咨询的蔬菜种植技术，我给您一些建议。有机蔬菜种植时，要选择优质有机种子，避免使用化学农药和化肥。建议采用轮作制度来保持土壤肥力，使用生物防治方法控制病虫害。还要注意合理灌溉，避免土壤过湿或过干。常见蔬菜的种植时间也有讲究，春季适合种菠菜、生菜、油菜、芹菜等，夏季适合种黄瓜、西红柿、茄子、辣椒等，秋季适合种萝卜、白菜、西兰花、胡萝卜等，冬季则适合种大葱、大蒜、韭菜、香菜等。我们定期举办农业技术培训，您可以关注平台公告报名参加。";
        } else if (lowerQuery.contains("水果") || lowerQuery.contains("果树")) {
            return "关于您咨询的水果种植技术，我给您一些建议。果树栽培时，要选择适宜当地气候的品种。合理修剪，保持树冠通风透光。科学施肥，平衡氮磷钾比例。还要及时防治病虫害。果树管理有几个关键时期，春季要注意花期管理和疏花疏果，夏季要关注果实膨大期管理和病虫害防治，秋季要处理果实成熟期管理和采后处理，冬季则需要进行休眠期修剪和清园消毒。我们平台提供专业的农业技术指导服务，如果您有需要，可以联系我们的农业专家。";
        } else {
            return "关于您咨询的农业技术问题，我有几个建议。您可以关注我们平台的农业技术专栏，获取最新的种植技术资讯。也可以参加我们组织的农业技术培训活动。如果有具体问题，还可以联系我们的农业专家获取一对一指导。另外，您也可以查看相关的农业技术书籍和视频教程。我们与多家农业科研机构合作，会定期更新农业技术信息，欢迎您持续关注。";
        }
    }
    
    // 处理价格行情查询
    private String handlePriceQuery(String lowerQuery, String originalQuery) {
        return "关于您查询的农产品价格行情，我可以告诉您一些信息。您可以通过几种渠道查询价格，比如登录平台后进入价格行情栏目查看最新价格，或者关注我们的平台公众号获取每日价格推送，还可以订阅价格行情短信服务。农产品价格波动主要受几个因素影响，季节变化会导致不同季节的农产品价格差异较大，市场供需关系也会影响价格，供大于求时价格下跌，供不应求时价格上涨。天气因素也会影响农产品的产量和质量，从而影响价格。运输成本也是一个因素，燃油价格上涨会导致运输成本增加。近期的价格趋势方面，蔬菜类在春季价格逐渐回落，水果类的当季水果价格比较稳定，品质也不错，粮油类的价格基本稳定，波动比较小。如果您需要更详细的价格信息，欢迎联系我们的市场分析师。";
    }
    
    // 处理订单相关查询
    private String handleOrderQuery(String lowerQuery, String originalQuery) {
        if (lowerQuery.contains("查询") || lowerQuery.contains("状态")) {
            return "关于您的订单查询，您可以通过几种方式查询。登录账户后进入我的订单页面查看，或者联系客服提供订单号查询，还可以关注平台公众号，绑定账户后查询。订单状态有几种，待付款表示订单已创建，等待支付。待发货表示支付成功，等待商家发货。待收货表示商家已发货，等待收货。已完成表示订单已完成，已取消则表示订单已取消。如果您有其他问题，欢迎随时咨询。";
        } else if (lowerQuery.contains("支付") || lowerQuery.contains("下单")) {
            return "关于您的订单支付问题，我们提供多种支付方式。在线支付方面，支持微信支付、支付宝支付和银行卡支付。部分地区还支持货到付款。线下支付的话，可以选择银行转账或者现金支付。支付时有几点注意事项，建议您在30分钟内完成支付，否则订单会自动取消。支付成功后，您会收到支付成功的短信。如果遇到支付问题，请及时联系客服。感谢您的支持。";
        } else {
            return "关于您的订单问题，建议您登录账户后进入订单详情页面查看详细信息。如果有需要，也可以联系在线客服获取实时帮助。您还可以查看订单历史记录，了解订单的处理进度。我们的客服团队7*24小时为您服务，如果您有任何问题，欢迎随时联系。";
        }
    }
    
    // 处理账号相关查询
    private String handleAccountQuery(String lowerQuery, String originalQuery) {
        if (lowerQuery.contains("注册") || lowerQuery.contains("账号")) {
            return "关于您咨询的账号注册问题，注册流程很简单。打开平台首页后点击注册按钮，选择注册方式，可以用手机号注册或者微信、支付宝快捷注册。然后填写必要的信息，比如用户名、密码、验证码等。阅读并同意用户协议和隐私政策后，点击注册就能完成账号创建。注册成功后，您可以完善个人资料提高账号安全性，绑定手机号和邮箱方便找回密码，还可以申请成为商户，开启您的电商之旅。如果您在注册过程中遇到问题，欢迎联系客服。";
        } else if (lowerQuery.contains("登录") || lowerQuery.contains("密码")) {
            return "关于您咨询的账号登录问题，我们提供几种解决方法。您可以使用密码登录，输入用户名或手机号和密码登录。也可以使用快捷登录，用微信或支付宝扫码登录。如果您忘记了密码，可以点击忘记密码，通过手机号或邮箱找回。输入验证码后设置新密码，完成密码重置后重新登录。登录时要注意保护好您的账号和密码，不要泄露给他人。建议定期更换密码提高账号安全性，还可以开启两步验证增强账号保护。如果您遇到登录问题，欢迎联系客服获取帮助。";
        } else {
            return "关于您咨询的账号问题，建议您登录账户后进入个人中心查看和管理账号信息。完善个人资料可以提高账号的可信度。绑定手机号和邮箱可以方便接收重要通知。设置安全问题可以增强账号保护。如果您有更多账号相关的问题，欢迎联系我们的客服团队。";
        }
    }
    
    // 默认回复
    private String defaultResponse(String query) {
        return "感谢您的咨询！我是乡村振兴农产品销售平台的AI助手，很高兴为您服务。您可以咨询农产品相关信息，比如蔬菜、水果、粮油等优质农产品。也可以查询订单和物流，随时了解您的订单状态和物流信息。如果您有售后服务方面的问题，比如退款、退换货、投诉等，我也可以为您解答。另外，我还可以为您介绍助农活动，比如直播带货、采摘体验、展销会等。农业技术方面，比如种植、养殖、病虫害防治等技术咨询，我也能提供帮助。您还可以了解最新的农产品价格和市场趋势，以及账号管理相关的问题，比如注册、登录、密码重置等。请告诉我您的具体需求，我会为您提供专业的帮助。";
    }

    // ==================== RAG 检索增强生成方法 ====================

    /**
     * RAG 增强查询（非流式）。
     * <p>
     * 完整 RAG 管道流程：
     * <ol>
     *   <li>多轮对话会话管理（创建/恢复）</li>
     *   <li>RAG 检索：查询向量化 → 知识库检索 → 组装上下文</li>
     *   <li>RAG 增强：构建增强系统提示词（知识上下文 + 对话历史）</li>
     *   <li>LLM 生成：调用 DeepSeek API 生成回答</li>
     *   <li>溯源记录：保存知识来源与检索元数据</li>
     * </ol>
     * </p>
     *
     * @param userId       用户 ID
     * @param query        用户查询
     * @param serviceType  服务类型
     * @param sessionToken 会话令牌（多轮对话，可空）
     * @return 包含回答、知识来源、检索信息的 Map
     */
    public Map<String, Object> handleRagQuery(Long userId, String query, Integer serviceType, String sessionToken) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();

        try {
            if (query == null || query.trim().isEmpty()) query = "推荐一些农产品";

            // ===== 问题7：意图识别与动态路由 =====
            IntentClassifierService.ClassificationResult intentResult = null;
            if (intentClassifierService != null) {
                try {
                    intentResult = intentClassifierService.classify(query);
                    if (serviceType == null) {
                        serviceType = intentClassifierService.getServiceType(intentResult.intent);
                    }
                    if (ragMonitorService != null) {
                        ragMonitorService.recordIntent(intentResult.intent.name());
                        ragMonitorService.recordQuery(query);
                    }
                    result.put("intent", intentResult.intent.name());
                    result.put("intentLabel", intentResult.intent.label);
                    result.put("intentConfidence", Math.round(intentResult.confidence * 10000) / 10000.0);
                } catch (Exception e) {
                    logger.warn("意图识别失败: {}", e.getMessage());
                }
            }

            if (serviceType == null || serviceType < 1 || serviceType > 3) serviceType = 1;

            // ===== 问题3：敏感信息过滤 =====
            String filteredQuery = query;
            if (contentFilterService != null) {
                try {
                    filteredQuery = contentFilterService.filterUserQuery(query);
                } catch (Exception e) {
                    logger.warn("查询过滤失败: {}", e.getMessage());
                }
            }

            // ===== 问题4：多级查询匹配（精确匹配 → 向量检索 → LLM） =====
            int matchLevel = 3;
            if (seedFAQInitializer != null) {
                try {
                    matchLevel = seedFAQInitializer.getMatchLevel(filteredQuery);
                    result.put("matchLevel", matchLevel);
                } catch (Exception e) {
                    logger.debug("匹配级别检测失败: {}", e.getMessage());
                }
            }

            // 1. 多轮对话会话管理
            com.example.minimall.model.ConversationSession session = null;
            String conversationContext = null;
            if (ragService.isRagEnabled() && conversationService != null) {
                session = conversationService.getOrCreateSession(sessionToken, userId, serviceType);
                conversationService.addUserMessage(session.getId(), query);
                conversationContext = conversationService.buildConversationContext(session.getId());
                result.put("sessionToken", session.getSessionToken());
            }

            // 2. 商品上下文（问题6：使用优化器进行相关性排序+Token控制）
            List<Product> allProducts = null;
            try {
                allProducts = productService.listAll();
            } catch (Exception e) {
                allProducts = new ArrayList<>();
            }
            List<Product> limitedProducts = filterProductsByQuery(allProducts, filteredQuery);
            String productContext;
            if (productContextOptimizer != null) {
                try {
                    List<Product> ranked = productContextOptimizer.rankAndSelect(limitedProducts, filteredQuery);
                    productContext = productContextOptimizer.buildOptimizedContext(ranked);
                    if (contentFilterService != null) {
                        productContext = contentFilterService.filterProductContext(productContext);
                    }
                } catch (Exception e) {
                    logger.warn("商品上下文优化失败，降级: {}", e.getMessage());
                    productContext = buildProductContext(limitedProducts);
                }
            } else {
                productContext = buildProductContext(limitedProducts);
            }

            // 3. RAG 检索
            RagService.RetrievalResult retrievalResult = null;
            String ragContext = null;
            if (ragService.isRagEnabled()) {
                long retrievalStart = System.currentTimeMillis();
                retrievalResult = ragService.retrieve(filteredQuery);
                long retrievalDuration = System.currentTimeMillis() - retrievalStart;
                ragContext = retrievalResult.contextText;
                result.put("retrievalScore", retrievalResult.topScore);
                result.put("retrievalTimeMs", retrievalResult.retrievalTimeMs);
                result.put("sources", retrievalResult.sources);
                result.put("sourceCount", retrievalResult.sources.size());
                logger.info("[RAG查询] 检索完成: score={}, time={}ms, sources={}",
                        retrievalResult.topScore, retrievalResult.retrievalTimeMs,
                        retrievalResult.sources.size());

                // ===== 问题5：监控埋点 =====
                if (ragMonitorService != null) {
                    ragMonitorService.recordRetrieval(retrievalDuration);
                    // 检索命中/未命中
                    boolean hit = !retrievalResult.chunks.isEmpty() || !retrievalResult.faqs.isEmpty();
                    ragMonitorService.recordRagResult(hit);
                }
            }

            // 4. 构建增强系统提示词
            String systemPrompt;
            if (ragService.isRagEnabled() && ragContext != null) {
                systemPrompt = ragService.buildRagSystemPrompt(serviceType, productContext, ragContext, conversationContext);
            } else {
                systemPrompt = buildSystemPrompt(serviceType, productContext);
            }

            // 5. 调用 DeepSeek API
            String response;
            long llmStart = System.currentTimeMillis();
            boolean llmSuccess = false;
            try {
                response = callDeepSeekAPIWithPrompt(filteredQuery, systemPrompt);
                llmSuccess = true;
            } catch (Exception e) {
                logger.warn("DeepSeek API调用失败，使用本地回复: {}", e.getMessage());
                try {
                    response = generateAIResponseWithUser(filteredQuery, serviceType, userId);
                    llmSuccess = true;
                } catch (Exception ex) {
                    response = defaultResponse(filteredQuery);
                }
            } finally {
                if (ragMonitorService != null) {
                    ragMonitorService.recordLlmCall(System.currentTimeMillis() - llmStart, llmSuccess);
                }
            }
            if (response == null || response.isEmpty()) {
                response = defaultResponse(filteredQuery);
            }

            long responseTime = System.currentTimeMillis() - startTime;

            // 6. 记录对话消息（含溯源信息）
            if (session != null) {
                String sourcesJson = retrievalResult != null ? ragService.serializeRetrievalResult(retrievalResult) : null;
                conversationService.addAssistantMessage(session.getId(), response, sourcesJson,
                        null, null,
                        retrievalResult != null ? java.math.BigDecimal.valueOf(retrievalResult.topScore) : null,
                        (int) responseTime);
            }

            // 7. 记录 AI 服务日志
            AIServiceLog log = new AIServiceLog();
            log.setUserId(userId != null && userId != 0L ? userId : null);
            log.setQuery(query);
            log.setResponse(response);
            log.setServiceType(serviceType);
            log.setCreatedAt(LocalDateTime.now());
            aiServiceLogMapper.insert(log);

            result.put("response", response);
            result.put("logId", log.getId());
            result.put("responseTimeMs", responseTime);
            result.put("ragEnabled", ragService.isRagEnabled());

            // 商品卡片（serviceType=1 时）
            if (serviceType == 1) {
                List<Map<String, Object>> productCards = buildProductCards(
                        getRelatedProducts(filteredQuery, response), response);
                if (!productCards.isEmpty()) {
                    result.put("productCards", productCards);
                }
            }

            return result;
        } catch (Exception e) {
            logger.error("RAG查询处理失败: {}", e.getMessage(), e);
            result.put("response", "抱歉，暂时无法回答您的问题，请稍后重试");
            result.put("ragEnabled", ragService.isRagEnabled());
            result.put("responseTimeMs", System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * RAG 增强查询（SSE 流式输出）。
     * <p>
     * 与 handleRagQuery 流程一致，但通过 SseEmitter 实时推送：
     * 1. 先推送检索事件（知识来源信息）
     * 2. 流式推送生成的 token
     * 3. 推送完成事件（含来源、商品卡片等）
     * </p>
     */
    public SseEmitter handleRagQueryStream(Long userId, String query, Integer serviceType, String sessionToken) {
        SseEmitter emitter = new SseEmitter(120000L);

        // 注册超时/错误回调，确保客户端断开时及时清理
        emitter.onTimeout(() -> {
            logger.warn("[RAG流式] 客户端超时断开");
            emitter.complete();
        });
        emitter.onError(e -> {
            logger.warn("[RAG流式] 客户端异常断开: {}", e.getMessage());
            emitter.complete();
        });

        if (query == null || query.trim().isEmpty()) query = "推荐一些农产品";

        // ===== 问题7：意图识别与动态路由 =====
        IntentClassifierService.ClassificationResult intentResult = null;
        if (intentClassifierService != null) {
            try {
                intentResult = intentClassifierService.classify(query);
                if (serviceType == null) {
                    serviceType = intentClassifierService.getServiceType(intentResult.intent);
                }
                if (ragMonitorService != null) {
                    ragMonitorService.recordIntent(intentResult.intent.name());
                    ragMonitorService.recordQuery(query);
                }
            } catch (Exception e) {
                logger.warn("[RAG流式] 意图识别失败: {}", e.getMessage());
            }
        }

        if (serviceType == null || serviceType < 1 || serviceType > 3) serviceType = 1;

        // ===== 问题3：敏感信息过滤 =====
        String filteredQuery = query;
        if (contentFilterService != null) {
            try {
                filteredQuery = contentFilterService.filterUserQuery(query);
            } catch (Exception e) {
                logger.warn("[RAG流式] 查询过滤失败: {}", e.getMessage());
            }
        }

        final Integer finalServiceType = serviceType;
        final String finalQuery = filteredQuery;
        final String originalQuery = query;
        final Long finalUserId = userId;
        final IntentClassifierService.Intent finalIntent = intentResult != null ? intentResult.intent : null;

        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            StringBuilder fullResponse = new StringBuilder();
            try {
                logger.info("[RAG流式] 开始处理, query={}, filtered={}, serviceType={}, intent={}",
                        originalQuery, finalQuery, finalServiceType,
                        finalIntent != null ? finalIntent.label : "unknown");

                // 1. 多轮对话会话
                com.example.minimall.model.ConversationSession session = null;
                String conversationContext = null;
                if (ragService.isRagEnabled() && conversationService != null) {
                    session = conversationService.getOrCreateSession(sessionToken, finalUserId, finalServiceType);
                    conversationService.addUserMessage(session.getId(), originalQuery);
                    conversationContext = conversationService.buildConversationContext(session.getId());
                }

                // 2. 商品上下文（问题6：使用优化器）
                List<Product> allProducts;
                try {
                    allProducts = productService.listAll();
                } catch (Exception e) {
                    logger.warn("[RAG流式] 加载商品列表失败: {}", e.getMessage());
                    allProducts = new ArrayList<>();
                }
                if (allProducts == null) allProducts = new ArrayList<>();
                List<Product> limitedProducts = filterProductsByQuery(allProducts, finalQuery);
                String productContext;
                if (productContextOptimizer != null) {
                    try {
                        List<Product> ranked = productContextOptimizer.rankAndSelect(limitedProducts, finalQuery);
                        productContext = productContextOptimizer.buildOptimizedContext(ranked);
                        if (contentFilterService != null) {
                            productContext = contentFilterService.filterProductContext(productContext);
                        }
                    } catch (Exception e) {
                        logger.warn("[RAG流式] 商品上下文优化失败，降级: {}", e.getMessage());
                        productContext = buildProductContext(limitedProducts);
                    }
                } else {
                    productContext = buildProductContext(limitedProducts);
                }

                // 3. RAG 检索
                RagService.RetrievalResult retrievalResult = null;
                String ragContext = null;
                if (ragService.isRagEnabled()) {
                    long retrievalStart = System.currentTimeMillis();
                    retrievalResult = ragService.retrieve(finalQuery);
                    long retrievalDuration = System.currentTimeMillis() - retrievalStart;
                    ragContext = retrievalResult.contextText;

                    // ===== 问题5：监控埋点 =====
                    if (ragMonitorService != null) {
                        ragMonitorService.recordRetrieval(retrievalDuration);
                        boolean hit = !retrievalResult.chunks.isEmpty() || !retrievalResult.faqs.isEmpty();
                        ragMonitorService.recordRagResult(hit);
                    }

                    // 推送检索事件（前端可展示知识来源）
                    Map<String, Object> retrievalEvent = new HashMap<>();
                    retrievalEvent.put("sources", retrievalResult.sources);
                    retrievalEvent.put("topScore", retrievalResult.topScore);
                    retrievalEvent.put("retrievalTimeMs", retrievalResult.retrievalTimeMs);
                    retrievalEvent.put("sourceCount", retrievalResult.sources.size());
                    if (finalIntent != null) {
                        retrievalEvent.put("intent", finalIntent.name());
                        retrievalEvent.put("intentLabel", finalIntent.label);
                    }
                    if (session != null) {
                        retrievalEvent.put("sessionToken", session.getSessionToken());
                    }
                    emitter.send(SseEmitter.event().name("retrieval").data(retrievalEvent));
                    logger.info("[RAG流式] 推送检索事件: {} 个来源", retrievalResult.sources.size());
                }

                // 4. 构建增强提示词
                String systemPrompt;
                if (ragContext != null) {
                    systemPrompt = ragService.buildRagSystemPrompt(finalServiceType, productContext, ragContext, conversationContext);
                } else {
                    systemPrompt = buildSystemPrompt(finalServiceType, productContext);
                }

                // 5. 流式调用 DeepSeek API
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", deepSeekConfig.getModel());
                requestBody.put("temperature", deepSeekConfig.getTemperature());
                requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());
                requestBody.put("stream", true);

                Map<String, String> thinking = new HashMap<>();
                thinking.put("type", "disabled");
                requestBody.put("thinking", thinking);

                List<Map<String, String>> messages = new ArrayList<>();
                Map<String, String> sysMsg = new HashMap<>();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);
                Map<String, String> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", finalQuery);
                messages.add(userMsg);
                requestBody.put("messages", messages);

                String requestJson = OBJECT_MAPPER.writeValueAsString(requestBody);
                org.apache.http.client.methods.HttpPost httpPost =
                        new org.apache.http.client.methods.HttpPost(deepSeekConfig.getApiUrl());
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Authorization", "Bearer " + deepSeekConfig.getApiKey());
                httpPost.setEntity(new org.apache.http.entity.StringEntity(requestJson, "UTF-8"));

                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(10000).setSocketTimeout(60000).build();

                try (CloseableHttpClient httpClient = HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig).build()) {
                    org.apache.http.HttpResponse response = httpClient.execute(httpPost);
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode != 200) {
                        throw new RuntimeException("DeepSeek API返回错误状态码: " + statusCode);
                    }

                    // 使用 try-with-resources 确保 reader 在异常时也能正确关闭，避免连接泄漏
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent(), "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                                String jsonData = line.substring(6);
                                try {
                                    JsonNode node = OBJECT_MAPPER.readTree(jsonData);
                                    JsonNode choices = node.get("choices");
                                    if (choices != null && choices.isArray() && choices.size() > 0) {
                                        JsonNode delta = choices.get(0).get("delta");
                                        if (delta != null) {
                                            JsonNode content = delta.get("content");
                                            if (content != null && content.isTextual()) {
                                                String token = content.asText();
                                                fullResponse.append(token);
                                                Map<String, String> event = new HashMap<>();
                                                event.put("token", token);
                                                emitter.send(SseEmitter.event().name("token").data(event));
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.warn("解析SSE数据失败: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }

                String cleanedResponse = fullResponse.toString()
                        .replaceAll("###+", " ")
                        .replaceAll("\\*\\*", "")
                        .replaceAll("__", "")
                        .replaceAll("(?m)^- ", "")
                        .replaceAll("(?m)^#+ ", "")
                        .replaceAll("\\n\\s+\\n", "\\n")
                        .trim();

                boolean llmSuccess = true;
                if (cleanedResponse.isEmpty()) {
                    cleanedResponse = generateAIResponseWithUser(finalQuery, finalServiceType, finalUserId);
                    llmSuccess = false;
                }

                // ===== 问题5：LLM 监控埋点 =====
                if (ragMonitorService != null) {
                    ragMonitorService.recordLlmCall(System.currentTimeMillis() - startTime, llmSuccess);
                }

                long responseTime = System.currentTimeMillis() - startTime;

                // 6. 记录对话消息
                if (session != null) {
                    String sourcesJson = retrievalResult != null ? ragService.serializeRetrievalResult(retrievalResult) : null;
                    conversationService.addAssistantMessage(session.getId(), cleanedResponse, sourcesJson,
                            null, null,
                            retrievalResult != null ? java.math.BigDecimal.valueOf(retrievalResult.topScore) : null,
                            (int) responseTime);
                }

                // 7. 推送完成事件
                Map<String, Object> doneEvent = new HashMap<>();
                doneEvent.put("response", cleanedResponse);
                doneEvent.put("responseTimeMs", responseTime);
                doneEvent.put("ragEnabled", ragService.isRagEnabled());
                if (finalIntent != null) {
                    doneEvent.put("intent", finalIntent.name());
                    doneEvent.put("intentLabel", finalIntent.label);
                }
                if (retrievalResult != null) {
                    doneEvent.put("sources", retrievalResult.sources);
                    doneEvent.put("sourceCount", retrievalResult.sources.size());
                    doneEvent.put("retrievalScore", retrievalResult.topScore);
                }
                if (session != null) {
                    doneEvent.put("sessionToken", session.getSessionToken());
                }

                if (finalServiceType == 1) {
                    List<Map<String, Object>> productCards = buildProductCards(
                            getRelatedProducts(finalQuery, cleanedResponse), cleanedResponse);
                    if (!productCards.isEmpty()) {
                        doneEvent.put("productCards", productCards);
                    }
                }

                if (isDiscountQuery(finalQuery)) {
                    List<DiscountActivity> activities = getRelatedActivities();
                    if (activities != null && !activities.isEmpty()) {
                        doneEvent.put("activityCards", buildActivityCards(activities));
                    }
                }

                emitter.send(SseEmitter.event().name("done").data(doneEvent));

                // 记录 AI 服务日志（记录原始查询，便于审计）
                AIServiceLog log = new AIServiceLog();
                log.setUserId(finalUserId != null && finalUserId != 0L ? finalUserId : null);
                log.setQuery(originalQuery);
                log.setResponse(cleanedResponse);
                log.setServiceType(finalServiceType);
                log.setCreatedAt(LocalDateTime.now());
                aiServiceLogMapper.insert(log);

                emitter.complete();

            } catch (Exception e) {
                logger.error("[RAG流式] 处理失败，使用降级回复", e);
                try {
                    String fallback = generateAIResponseWithUser(finalQuery, finalServiceType, finalUserId);
                    if (fallback == null || fallback.isEmpty()) {
                        fallback = "很抱歉，我暂时无法处理您的请求。您可以尝试重新提问，或联系在线客服获取帮助。";
                    }
                    Map<String, Object> doneEvent = new HashMap<>();
                    doneEvent.put("response", fallback);
                    doneEvent.put("ragEnabled", false);
                    doneEvent.put("responseTimeMs", System.currentTimeMillis() - startTime);

                    if (finalServiceType == 1) {
                        try {
                            List<Map<String, Object>> productCards = buildProductCards(
                                    getRelatedProducts(finalQuery, fallback), fallback);
                            if (!productCards.isEmpty()) doneEvent.put("productCards", productCards);
                        } catch (Exception ignored) {}
                    }
                    emitter.send(SseEmitter.event().name("done").data(doneEvent));
                    emitter.complete();
                } catch (Exception ex) {
                    logger.error("[RAG流式] 降级处理完全失败: {}", ex.getMessage());
                    try { emitter.complete(); } catch (Exception ignored) {}
                }
            }
        });

        return emitter;
    }

    /**
     * 使用指定系统提示词调用 DeepSeek API
     */
    private String callDeepSeekAPIWithPrompt(String query, String systemPrompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepSeekConfig.getModel());
        requestBody.put("temperature", deepSeekConfig.getTemperature());
        requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", query);
        messages.add(userMessage);

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekConfig.getApiKey());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                deepSeekConfig.getApiUrl(), requestEntity, String.class);

        ObjectMapper mapper = OBJECT_MAPPER;
        JsonNode rootNode = mapper.readTree(responseEntity.getBody());
        JsonNode choicesNode = rootNode.get("choices");
        if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
            JsonNode messageNode = choicesNode.get(0).get("message");
            if (messageNode != null) {
                JsonNode contentNode = messageNode.get("content");
                if (contentNode != null && contentNode.isTextual()) {
                    return contentNode.asText()
                            .replaceAll("###+", " ")
                            .replaceAll("\\*\\*", "")
                            .replaceAll("__", "")
                            .replaceAll("(?m)^- ", "")
                            .replaceAll("(?m)^#+ ", "")
                            .replaceAll("\\n\\s+\\n", "\\n")
                            .trim();
                }
            }
        }
        return defaultResponse(query);
    }

    /**
     * 查询用户的所有 AI 服务日志
     *
     * @param userId 用户 ID
     * @return 日志列表
     */
    public List<AIServiceLog> getLogsByUserId(Long userId) {
        return aiServiceLogMapper.selectByUserId(userId);
    }

    /**
     * 分页查询 AI 服务日志（管理后台用）
     *
     * @param page        页码
     * @param size        每页大小
     * @param userId      用户 ID（可空）
     * @param serviceType 服务类型（可空）
     * @return 日志分页
     */
    public IPage<AIServiceLog> getLogsPage(int page, int size, Long userId, Integer serviceType) {
        Page<AIServiceLog> pageQuery = new Page<>(page, size);
        return aiServiceLogMapper.selectPage(pageQuery, userId, serviceType);
    }

    /**
     * 根据 ID 查询 AI 服务日志详情
     *
     * @param id 日志 ID
     * @return 日志实体
     */
    public AIServiceLog getLogById(Long id) {
        return aiServiceLogMapper.selectById(id);
    }

    /**
     * 删除 AI 服务日志
     *
     * @param id 日志 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLog(Long id) {
        aiServiceLogMapper.deleteById(id);
    }
    
    /** 批量删除 AI 服务日志 */
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteLogs(List<Long> ids) {
        for (Long id : ids) {
            aiServiceLogMapper.deleteById(id);
        }
    }
    
    /** 清空全部 AI 服务日志 */
    @Transactional(rollbackFor = Exception.class)
    public void clearLogs() {
        aiServiceLogMapper.deleteAll();
    }
    
    // 清空指定用户的AI服务日志
    @Transactional(rollbackFor = Exception.class)
    public void clearLogsByUserId(Long userId) {
        aiServiceLogMapper.deleteByUserId(userId);
    }
    
    /** 清空指定类型的 AI 服务日志 */
    @Transactional(rollbackFor = Exception.class)
    public void clearLogsByServiceType(Integer serviceType) {
        aiServiceLogMapper.deleteByServiceType(serviceType);
    }
}

