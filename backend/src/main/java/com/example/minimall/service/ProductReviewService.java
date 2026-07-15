package com.example.minimall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.mapper.ProductReviewMapper;
import com.example.minimall.model.ProductReview;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** 商品评价服务 */
@Service
public class ProductReviewService {
    /** 商品评价 Mapper */
    private final ProductReviewMapper productReviewMapper;

    public ProductReviewService(ProductReviewMapper productReviewMapper) {
        this.productReviewMapper = productReviewMapper;
    }

    /**
     * 创建商品评价（**事务**）
     * <p>
     * 校验：
     * <ul>
     *   <li>评分必须 1-5</li>
     *   <li>同一订单同一商品不能重复评价</li>
     * </ul>
     * 评价创建后默认 status=1（有效）
     * </p>
     *
     * @param review 评价实体
     * @return 创建后的评价
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductReview createReview(ProductReview review) {
        // 检查评分是否合法
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("评分必须在1-5之间");
        }

        // 检查是否已经评价过
        ProductReview existing = productReviewMapper.selectByOrderIdAndProductId(review.getOrderId(), review.getProductId());
        if (existing != null) {
            throw new IllegalArgumentException("该订单商品已经评价过");
        }

        review.setStatus(1); // 状态：有效
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        productReviewMapper.insert(review);
        return review;
    }

    /**
     * 查询某商品的全部评价
     *
     * @param productId 商品 ID
     * @return 评价列表
     */
    public List<ProductReview> getReviewsByProductId(Long productId) {
        return productReviewMapper.selectByProductId(productId);
    }

    /**
     * 分页查询某商品的评价
     *
     * @param productId 商品 ID
     * @param page      当前页（1 开始）
     * @param size      每页大小
     * @return 评价分页对象
     */
    public IPage<ProductReview> getReviewsByProductIdPage(Long productId, int page, int size) {
        Page<ProductReview> pageQuery = new Page<>(page, size);
        return productReviewMapper.selectByProductIdPage(pageQuery, productId);
    }

    /**
     * 带筛选条件的分页查询评价
     * <p>支持按最低分/最高分/是否有图筛选</p>
     *
     * @param productId 商品 ID
     * @param page      当前页
     * @param size      每页大小
     * @param minRating 最低评分（可空）
     * @param maxRating 最高评分（可空）
     * @param hasImage  是否要求包含图片（可空）
     * @return 评价分页
     */
    public IPage<ProductReview> getReviewsByProductIdPageWithFilter(Long productId, int page, int size,
                                                                     Integer minRating, Integer maxRating,
                                                                     Boolean hasImage) {
        Page<ProductReview> pageQuery = new Page<>(page, size);
        return productReviewMapper.selectByProductIdPageWithFilter(pageQuery, productId, minRating, maxRating, hasImage);
    }

    /**
     * 查询某用户的所有评价
     *
     * @param userId 用户 ID
     * @return 评价列表
     */
    public List<ProductReview> getReviewsByUserId(Long userId) {
        return productReviewMapper.selectByUserId(userId);
    }

    /**
     * 根据评价 ID 查询
     *
     * @param id 评价主键
     * @return 评价实体
     */
    public ProductReview getReviewById(Long id) {
        return productReviewMapper.selectById(id);
    }

    /**
     * 更新评价（**事务**，部分字段更新）
     *
     * @param id     评价主键
     * @param review 新值（仅非空字段被采纳）
     * @return 更新后的评价实体
     */
    @Transactional(rollbackFor = Exception.class)
    public ProductReview updateReview(Long id, ProductReview review) {
        ProductReview existing = productReviewMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("评价不存在");
        }

        // 更新字段
        if (review.getRating() != null) {
            if (review.getRating() < 1 || review.getRating() > 5) {
                throw new IllegalArgumentException("评分必须在1-5之间");
            }
            existing.setRating(review.getRating());
        }
        if (review.getContent() != null) {
            existing.setContent(review.getContent());
        }
        if (review.getImages() != null) {
            existing.setImages(review.getImages());
        }
        if (review.getStatus() != null) {
            existing.setStatus(review.getStatus());
        }
        existing.setUpdatedAt(LocalDateTime.now());

        productReviewMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除评价（**事务**）
     *
     * @param id 评价主键
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long id) {
        productReviewMapper.deleteById(id);
    }

    /**
     * 计算商品平均评分
     * <p>无评价时返回 0.0</p>
     *
     * @param productId 商品 ID
     * @return 平均评分（保留 double 精度）
     */
    public double calculateAverageRating(Long productId) {
        List<ProductReview> reviews = productReviewMapper.selectByProductId(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }

        int total = 0;
        for (ProductReview review : reviews) {
            total += review.getRating();
        }
        return (double) total / reviews.size();
    }

    /**
     * 分页查询某商家的所有商品评价
     *
     * @param sellerId 商家 ID
     * @param page     当前页
     * @param size     每页大小
     * @param keyword  关键词（可空）
     * @return 评价分页
     */
    public IPage<ProductReview> getReviewsBySellerIdPage(Long sellerId, int page, int size, String keyword) {
        Page<ProductReview> pageQuery = new Page<>(page, size);
        return productReviewMapper.selectBySellerIdPage(pageQuery, sellerId, keyword);
    }

    /**
     * 商家评价统计
     * <p>统计项：total、goodCount（≥4）、mediumCount（=3）、badCount（≤2）、averageRating、repliedCount</p>
     *
     * @param sellerId 商家 ID
     * @return 统计 Map
     */
    public Map<String, Object> getSellerReviewStats(Long sellerId) {
        List<ProductReview> reviews = productReviewMapper.selectBySellerId(sellerId);
        
        Map<String, Object> stats = new HashMap<>();
        int total = reviews.size();
        int goodCount = 0;
        int mediumCount = 0;
        int badCount = 0;
        int totalRating = 0;
        int repliedCount = 0;
        
        for (ProductReview review : reviews) {
            totalRating += review.getRating();
            
            if (review.getRating() >= 4) {
                goodCount++;
            } else if (review.getRating() == 3) {
                mediumCount++;
            } else {
                badCount++;
            }
            
            // 检查是否有回复（需要后续加载）
            // 这里简单处理，实际应该在 Mapper 中关联查询
        }
        
        stats.put("total", total);
        stats.put("goodCount", goodCount);
        stats.put("mediumCount", mediumCount);
        stats.put("badCount", badCount);
        stats.put("averageRating", total > 0 ? (double) totalRating / total : 0.0);
        stats.put("repliedCount", repliedCount);
        
        return stats;
    }
}
