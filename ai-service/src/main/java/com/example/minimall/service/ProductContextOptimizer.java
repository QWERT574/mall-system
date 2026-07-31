package com.example.minimall.service;

import com.example.minimall.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品上下文优化器
 *
 * 解决问题：商品上下文无限膨胀
 *
 * 核心功能：
 * 1. 基于用户查询的商品相关性排序（TF-IDF + 分类匹配 + 销量加权）
 * 2. 商品信息摘要机制（提取关键属性，减少冗余）
 * 3. 上下文长度动态控制（基于Token估算，控制在模型上下文40%以内）
 */
@Service
public class ProductContextOptimizer {
    private static final Logger logger = LoggerFactory.getLogger(ProductContextOptimizer.class);

    // 中文Token估算：约1个汉字≈1.5个token，1个英文单词≈1个token
    private static final double CHINESE_CHAR_TOKEN_RATIO = 1.5;

    @Value("${rag.max-context-tokens:3000}")
    private int maxContextTokens;

    // 商品上下文最多占模型上下文的40%
    private static final double PRODUCT_CONTEXT_RATIO = 0.4;

    // 最大返回商品数
    @Value("${ai.product-context.max-items:15}")
    private int maxItems;

    // 单个商品描述最大长度
    @Value("${ai.product-context.max-desc-length:60}")
    private int maxDescLength;

    // 销量归一化基准（用于对数归一化的分母基准）
    private static final int DEFAULT_MAX_SALES = 1000;

    // isFeatured getter 反射缓存（避免对每个商品重复反射查找）
    private static volatile Method isFeaturedGetterCache;
    private static volatile boolean isFeaturedChecked = false;

    /**
     * 对商品列表进行相关性排序并截断
     *
     * @param products 所有候选商品
     * @param query    用户查询
     * @return 按相关性降序排列的商品列表（已截断）
     */
    public List<Product> rankAndSelect(List<Product> products, String query) {
        // 1. 空值检查：商品为空直接返回空列表
        if (products == null || products.isEmpty()) {
            logger.debug("商品列表为空，返回空列表");
            return Collections.emptyList();
        }

        // 查询为空时，直接返回前 maxItems 个商品（保持原顺序）
        if (query == null || query.trim().isEmpty()) {
            logger.debug("查询为空，返回前 {} 个商品（无相关性排序）", maxItems);
            int limit = Math.min(maxItems, products.size());
            return new ArrayList<>(products.subList(0, limit));
        }

        // 2. 计算每个商品的相关性得分并包装为带分数的条目
        List<ScoredProduct> scored = new ArrayList<>(products.size());
        for (Product p : products) {
            double score = calculateRelevanceScore(p, query);
            scored.add(new ScoredProduct(p, score));
        }

        // 3. 按得分降序排序（得分相同则保持稳定顺序）
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 4. 截断到 maxItems
        int limit = Math.min(maxItems, scored.size());
        List<Product> result = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            result.add(scored.get(i).product);
        }

        // 5. 日志记录
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("商品相关性排序结果（query=").append(query).append("）：");
            for (int i = 0; i < limit; i++) {
                ScoredProduct sp = scored.get(i);
                sb.append("\n  #").append(i + 1)
                        .append(" [score=").append(String.format("%.4f", sp.score)).append("] ")
                        .append(sp.product.getName());
            }
            logger.debug(sb.toString());
        }
        logger.info("商品排序完成：候选 {} 个，返回 {} 个", products.size(), result.size());

        return result;
    }

    /**
     * 计算商品与查询的相关性得分
     *
     * 得分组成：
     * - 名称匹配（权重0.4）：名称中包含查询关键词
     * - 描述匹配（权重0.3）：描述中包含查询关键词
     * - 推荐商品加权（权重0.1）：isFeatured=1加分
     * - 销量加权（权重0.2）：销量越高加分越多（对数归一化）
     *
     * @return 0-1之间的得分
     */
    private double calculateRelevanceScore(Product product, String query) {
        if (product == null) return 0.0;

        String productName = product.getName() == null ? "" : product.getName().toLowerCase();
        String productDesc = product.getDescription() == null ? "" : product.getDescription().toLowerCase();
        String q = query == null ? "" : query.toLowerCase().trim();

        List<String> keywords = extractKeywords(q);
        List<String> nameTokens = splitToTokens(productName);

        // --- 名称匹配（权重 0.4） ---
        double nameScore = 0.0;
        if (!q.isEmpty()) {
            // 情况1：query 中包含商品名称中的词
            for (String token : nameTokens) {
                if (token.length() >= 2 && q.contains(token)) {
                    nameScore += 1.0;
                }
            }
            // 情况2：商品名称包含 query 中的关键词
            for (String kw : keywords) {
                if (kw.length() >= 2 && productName.contains(kw)) {
                    nameScore += 1.0;
                }
            }
            // 情况3：query 直接包含整个商品名称（强匹配）
            if (productName.length() >= 2 && q.contains(productName)) {
                nameScore += 1.0;
            }
            // 归一化到 [0,1]：每命中一个得一分，封顶1.0
            int maxPossible = Math.max(1, keywords.size() + nameTokens.size() + 1);
            nameScore = Math.min(1.0, nameScore / Math.min(maxPossible, 3.0));
        }
        double nameWeighted = nameScore * 0.4;

        // --- 描述匹配（权重 0.3） ---
        double descScore = 0.0;
        if (!keywords.isEmpty() && !productDesc.isEmpty()) {
            int hit = 0;
            for (String kw : keywords) {
                if (kw.length() >= 2 && productDesc.contains(kw)) {
                    hit++;
                }
            }
            descScore = Math.min(1.0, (double) hit / keywords.size());
        }
        double descWeighted = descScore * 0.3;

        // --- 推荐商品加权（权重 0.1）：isFeatured=1 加分 ---
        double featuredWeighted = 0.0;
        int isFeatured = getIsFeatured(product);
        if (isFeatured == 1) {
            featuredWeighted = 0.1;
        }

        // --- 销量加权（权重 0.2）：log(sales+1)/log(maxSales+1) 对数归一化 ---
        int sales = product.getSales() == null ? 0 : product.getSales();
        double salesNorm;
        double denom = Math.log(DEFAULT_MAX_SALES + 1);
        if (denom <= 0) {
            salesNorm = 0.0;
        } else {
            salesNorm = Math.log(sales + 1) / denom;
        }
        // 限制到 [0,1]
        salesNorm = Math.max(0.0, Math.min(1.0, salesNorm));
        double salesWeighted = salesNorm * 0.2;

        double total = nameWeighted + descWeighted + featuredWeighted + salesWeighted;
        // 最终限制到 [0,1]
        return Math.max(0.0, Math.min(1.0, total));
    }

    /**
     * 构建优化后的商品上下文文本
     *
     * @param products 已排序的商品列表
     * @return 精简后的上下文文本
     */
    public String buildOptimizedContext(List<Product> products) {
        // 1. 为空返回提示
        if (products == null || products.isEmpty()) {
            return "【商品上下文】暂无相关商品信息。";
        }

        // 2. 计算 token 预算 = maxContextTokens * PRODUCT_CONTEXT_RATIO
        int tokenBudget = (int) Math.round(maxContextTokens * PRODUCT_CONTEXT_RATIO);

        StringBuilder sb = new StringBuilder();
        // 头部：分类概览
        sb.append("【商品上下文】共找到 ").append(products.size()).append(" 个候选商品，按相关性排序展示：\n");

        // 商品按分类分组概览
        Map<String, Long> categoryOverview = products.stream()
                .collect(Collectors.groupingBy(this::categoryKey, Collectors.counting()));
        sb.append("分类分布：");
        boolean first = true;
        for (Map.Entry<String, Long> e : categoryOverview.entrySet()) {
            if (!first) sb.append("，");
            sb.append(e.getKey()).append("(").append(e.getValue()).append(")");
            first = false;
        }
        sb.append("\n\n");

        int usedTokens = estimateTokens(sb.toString());
        int added = 0;
        int total = products.size();

        // 3. 逐个添加商品摘要，累计 token 数
        for (Product p : products) {
            String summary = generateProductSummary(p);
            int summaryTokens = estimateTokens(summary);
            // 预留 30 token 给尾部提示
            if (usedTokens + summaryTokens > tokenBudget - 30) {
                logger.debug("token 预算即将耗尽（used={}, budget={}），停止添加商品", usedTokens, tokenBudget);
                break;
            }
            sb.append(summary).append("\n");
            usedTokens += summaryTokens + 1; // +1 用于换行
            added++;
        }

        // 4. 添加"（已展示N/M个商品）"提示
        sb.append("\n（已展示 ").append(added).append("/").append(total).append(" 个商品）");

        logger.info("构建商品上下文完成：展示 {}/{}，使用 token 约 {}/{}", added, total, usedTokens, tokenBudget);
        return sb.toString();
    }

    /**
     * 生成单个商品的摘要信息
     */
    private String generateProductSummary(Product product) {
        if (product == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("ID:").append(product.getId()).append(" | ");

        // 商品名称
        String name = product.getName() == null ? "未命名" : product.getName();
        sb.append(name).append(" | ");

        // 价格
        BigDecimal price = product.getPrice();
        sb.append("¥").append(price != null ? price.toPlainString() : "0").append(" | ");

        // 精简描述：截断到 maxDescLength
        String desc = product.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            desc = desc.trim().replaceAll("\\s+", " ");
            if (desc.length() > maxDescLength) {
                desc = desc.substring(0, maxDescLength) + "...";
            }
            sb.append(desc);
        } else {
            sb.append("无描述");
        }

        // 附加销量与库存（轻量信息，辅助模型回答）
        Integer sales = product.getSales();
        Integer stock = product.getStock();
        sb.append(" | 销量:").append(sales != null ? sales : 0)
                .append(" 库存:").append(stock != null ? stock : 0);

        // 推荐标记（若存在 isFeatured=1）
        if (getIsFeatured(product) == 1) {
            sb.append(" [推荐]");
        }

        return sb.toString();
    }

    /**
     * 估算文本的token数
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (isChineseChar(c)) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        return (int) Math.ceil(chineseChars * CHINESE_CHAR_TOKEN_RATIO + otherChars / 4.0);
    }

    private boolean isChineseChar(char c) {
        return c >= '\u4e00' && c <= '\u9fff';
    }

    /**
     * 从查询中提取关键词
     *
     * 简单实现：按空格分词 + 2-gram，去重返回
     */
    private List<String> extractKeywords(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = query.trim().toLowerCase().replaceAll("\\s+", " ");
        Set<String> keywords = new LinkedHashSet<>();

        // 1. 按空格分词
        String[] words = normalized.split(" ");
        for (String w : words) {
            if (w.length() >= 2) {
                keywords.add(w);
            }
        }

        // 2. 生成 2-gram（针对无空格的中文查询特别有效）
        String compact = normalized.replace(" ", "");
        if (compact.length() >= 2) {
            for (int i = 0; i < compact.length() - 1; i++) {
                keywords.add(compact.substring(i, i + 2));
            }
        }
        // 整体作为关键词（用于精确子串匹配）
        if (compact.length() >= 2) {
            keywords.add(compact);
        }

        return new ArrayList<>(keywords);
    }

    /**
     * 获取优化统计信息
     */
    public Map<String, Object> getOptimizationStats(List<Product> original, List<Product> optimized, String context) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("originalCount", original == null ? 0 : original.size());
        stats.put("optimizedCount", optimized == null ? 0 : optimized.size());
        stats.put("contextLength", context == null ? 0 : context.length());
        stats.put("estimatedTokens", estimateTokens(context));
        stats.put("tokenBudget", (int) Math.round(maxContextTokens * PRODUCT_CONTEXT_RATIO));
        return stats;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 将文本切分为 token（用于名称匹配）
     */
    private List<String> splitToTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = text.trim().toLowerCase().replaceAll("\\s+", " ");
        List<String> tokens = new ArrayList<>();
        // 按空格切分
        for (String w : normalized.split(" ")) {
            if (w.length() >= 2) tokens.add(w);
        }
        // 中文 2-gram
        String compact = normalized.replace(" ", "");
        if (compact.length() >= 2) {
            for (int i = 0; i < compact.length() - 1; i++) {
                tokens.add(compact.substring(i, i + 2));
            }
        }
        return tokens;
    }

    /**
     * 获取商品的分类标识 key（用于分组概览）
     */
    private String categoryKey(Product p) {
        if (p == null || p.getCategoryId() == null) return "未分类";
        return "分类" + p.getCategoryId();
    }

    /**
     * 安全获取 Product 的 isFeatured 字段值。
     * <p>
     * 由于当前 Product 模型可能未声明 isFeatured 字段（不同版本差异），
     * 此处通过反射兼容性地访问，缺失时返回 0，避免编译/运行期错误。
     * </p>
     */
    private int getIsFeatured(Product product) {
        if (product == null) return 0;
        // 反射结果缓存：避免对每个商品重复反射查找（Product 无 isFeatured 字段时快速短路返回 0）
        Method cached = isFeaturedGetterCache;
        if (isFeaturedChecked && cached == null) {
            return 0; // 已确认 Product 不存在 isFeatured 字段
        }
        Method getter = cached;
        if (!isFeaturedChecked) {
            getter = findIsFeaturedGetter(product.getClass());
            isFeaturedGetterCache = getter;
            isFeaturedChecked = true;
        }
        if (getter == null) return 0;
        try {
            Object val = getter.invoke(product);
            if (val instanceof Boolean) {
                return ((Boolean) val) ? 1 : 0;
            }
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            return val == null ? 0 : Integer.parseInt(val.toString());
        } catch (Exception e) {
            logger.debug("获取 isFeatured 失败：{}", e.getMessage());
            return 0;
        }
    }

    /** 查找 Product 的 isFeatured getter，不存在时返回 null */
    private Method findIsFeaturedGetter(Class<?> clazz) {
        try {
            return clazz.getMethod("getIsFeatured");
        } catch (NoSuchMethodException e) {
            try {
                return clazz.getMethod("isFeatured");
            } catch (NoSuchMethodException e2) {
                return null;
            }
        }
    }

    /**
     * 带分数的商品包装类，用于排序
     */
    private static class ScoredProduct {
        final Product product;
        final double score;

        ScoredProduct(Product product, double score) {
            this.product = product;
            this.score = score;
        }
    }
}
