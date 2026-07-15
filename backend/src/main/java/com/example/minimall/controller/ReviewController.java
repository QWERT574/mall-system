package com.example.minimall.controller;

import com.example.minimall.model.ProductReview;
import com.example.minimall.model.ReviewReply;
import com.example.minimall.security.XssUtils;
import com.example.minimall.service.ProductReviewService;
import com.example.minimall.service.ReviewReplyService;
import com.example.minimall.utils.JwtUtil;
import com.example.minimall.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class ReviewController {
    /** 商品评价业务服务 */
    private final ProductReviewService productReviewService;
    /** 评价回复业务服务 */
    private final ReviewReplyService reviewReplyService;
    /** JWT 工具，用于从请求中解析用户身份 */
    private final JwtUtil jwtUtil;
    /** 用户业务服务 */
    private final UserService userService;

    public ReviewController(ProductReviewService productReviewService, ReviewReplyService reviewReplyService,
                            JwtUtil jwtUtil, UserService userService) {
        this.productReviewService = productReviewService;
        this.reviewReplyService = reviewReplyService;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    // 创建评价
    @PostMapping
    public Map<String, Object> createReview(@RequestBody ProductReview review) {
        try {
            if (review.getContent() != null) {
                review.setContent(XssUtils.sanitize(review.getContent()));
            }
            ProductReview created = productReviewService.createReview(review);
            return createSuccessResponse(created);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("创建评价失败: " + e.getMessage());
        }
    }

    // 根据商家 ID 查询评价列表（商家端）
    @GetMapping("/seller/{sellerId}")
    public Map<String, Object> getReviewsBySellerId(@PathVariable Long sellerId,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "20") Integer size,
                                                    @RequestParam(required = false) String keyword) {
        try {
            // 获取商家所有商品的评价
            com.baomidou.mybatisplus.core.metadata.IPage<ProductReview> reviews = productReviewService.getReviewsBySellerIdPage(
                sellerId, page, size, keyword);
            
            // 获取每条评价的回复
            List<ProductReview> records = reviews.getRecords();
            for (ProductReview review : records) {
                List<ReviewReply> replies = reviewReplyService.getRepliesByReviewId(review.getId());
                if (!replies.isEmpty()) {
                    review.setReply(replies.get(0));
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", reviews.getTotal());
            result.put("records", records);
            result.put("current", reviews.getCurrent());
            result.put("size", reviews.getSize());
            
            return createSuccessResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取商家评价列表失败：" + e.getMessage());
        }
    }
    
    // 批量提交订单评价
    @PostMapping("/submit")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitOrderReview(@RequestBody Map<String, Object> reviewData,
                                                  HttpServletRequest request) {
        try {
            Long orderId = Long.parseLong(reviewData.get("orderId").toString());
            List<Map<String, Object>> items = (List<Map<String, Object>>) reviewData.get("items");

            if (items == null || items.isEmpty()) {
                return createErrorResponse("评价商品不能为空");
            }

            // 从JWT Token获取当前用户ID
            Long currentUserId = getUserIdFromToken(request);
            // 允许前端传入userId作为后备（兼容小程序）
            Long userId = currentUserId;
            if (userId == null && reviewData.get("userId") != null) {
                userId = Long.parseLong(reviewData.get("userId").toString());
            }
            if (userId == null) {
                return createErrorResponse("请先登录");
            }

            final Long finalUserId = userId;
            // 为每个商品创建评价
            for (Map<String, Object> item : items) {
                ProductReview review = new ProductReview();
                review.setOrderId(orderId);
                review.setProductId(Long.parseLong(item.get("productId").toString()));
                review.setUserId(finalUserId);
                review.setRating(Integer.parseInt(item.get("rating").toString()));
                String comment = item.get("comment") != null ? item.get("comment").toString() : null;
                review.setContent(comment != null ? XssUtils.sanitize(comment) : null);
                review.setImages(item.get("images") != null ? item.get("images").toString() : null);
                review.setStatus(1); // 审核通过

                productReviewService.createReview(review);
            }

            return createSuccessResponse("评价提交成功");
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("评价提交失败：" + e.getMessage());
        }
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        try {
            String bearerToken = request.getHeader("Authorization");
            if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
                return null;
            }
            String token = bearerToken.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
    
    // 获取商家评价统计（商家端）
    @GetMapping("/seller/{sellerId}/stats")
    public Map<String, Object> getSellerReviewStats(@PathVariable Long sellerId) {
        try {
            Map<String, Object> stats = productReviewService.getSellerReviewStats(sellerId);
            return createSuccessResponse(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取商家评价统计失败：" + e.getMessage());
        }
    }
    
    // 根据商品 ID 查询评价列表
    @GetMapping("/product/{productId}")
    public Map<String, Object> getReviewsByProductId(@PathVariable Long productId, 
                                                   @RequestParam(defaultValue = "1") Integer page, 
                                                   @RequestParam(defaultValue = "10") Integer size) {
        try {
            // 计算平均评分
            double avgRating = productReviewService.calculateAverageRating(productId);
            // 获取分页评价列表
            com.baomidou.mybatisplus.core.metadata.IPage<ProductReview> reviews = productReviewService.getReviewsByProductIdPage(productId, page, size);
            
            // 获取每条评价的商家回复
            List<ProductReview> records = reviews.getRecords();
            for (ProductReview review : records) {
                List<ReviewReply> replies = reviewReplyService.getRepliesByReviewId(review.getId());
                if (!replies.isEmpty()) {
                    review.setReply(replies.get(0));
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("averageRating", avgRating);
            result.put("total", reviews.getTotal());
            result.put("records", records);
            result.put("current", reviews.getCurrent());
            result.put("size", reviews.getSize());
            
            return createSuccessResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取评价列表失败: " + e.getMessage());
        }
    }

    // 根据用户ID查询评价列表
    @GetMapping("/user/{userId}")
    public Map<String, Object> getReviewsByUserId(@PathVariable Long userId) {
        try {
            List<ProductReview> reviews = productReviewService.getReviewsByUserId(userId);
            return createSuccessResponse(reviews);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取用户评价列表失败: " + e.getMessage());
        }
    }

    // 根据ID查询评价
    @GetMapping("/{id}")
    public Map<String, Object> getReviewById(@PathVariable Long id) {
        try {
            ProductReview review = productReviewService.getReviewById(id);
            if (review != null) {
                // 查询评价回复
                List<ReviewReply> replies = reviewReplyService.getRepliesByReviewId(id);
                Map<String, Object> result = new HashMap<>();
                result.put("review", review);
                result.put("replies", replies);
                return createSuccessResponse(result);
            } else {
                return createErrorResponse("评价不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取评价详情失败: " + e.getMessage());
        }
    }

    // 更新评价
    @PutMapping("/{id}")
    public Map<String, Object> updateReview(@PathVariable Long id, @RequestBody ProductReview review) {
        try {
            if (review.getContent() != null) {
                review.setContent(XssUtils.sanitize(review.getContent()));
            }
            ProductReview updated = productReviewService.updateReview(id, review);
            return createSuccessResponse(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新评价失败: " + e.getMessage());
        }
    }

    // 删除评价
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteReview(@PathVariable Long id) {
        try {
            productReviewService.deleteReview(id);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("删除评价失败: " + e.getMessage());
        }
    }

    // 创建评价回复
    @PostMapping("/reply")
    public Map<String, Object> createReply(@RequestBody ReviewReply reply) {
        try {
            ReviewReply created = reviewReplyService.createReply(reply);
            return createSuccessResponse(created);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("创建回复失败: " + e.getMessage());
        }
    }

    // 根据评价ID查询回复列表
    @GetMapping("/reply/{reviewId}")
    public Map<String, Object> getRepliesByReviewId(@PathVariable Long reviewId) {
        try {
            List<ReviewReply> replies = reviewReplyService.getRepliesByReviewId(reviewId);
            return createSuccessResponse(replies);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取回复列表失败: " + e.getMessage());
        }
    }

    // 更新回复
    @PutMapping("/reply/{id}")
    public Map<String, Object> updateReply(@PathVariable Long id, @RequestBody ReviewReply reply) {
        try {
            ReviewReply updated = reviewReplyService.updateReply(id, reply);
            return createSuccessResponse(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新回复失败: " + e.getMessage());
        }
    }

    // 删除回复
    @DeleteMapping("/reply/{id}")
    public Map<String, Object> deleteReply(@PathVariable Long id) {
        try {
            reviewReplyService.deleteReply(id);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("删除回复失败: " + e.getMessage());
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
