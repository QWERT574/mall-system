package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.ProductMapper;
import com.example.minimall.mapper.ProductSpecMapper;
import com.example.minimall.model.Category;
import com.example.minimall.model.DiscountActivity;
import com.example.minimall.model.Product;
import com.example.minimall.model.ProductSpec;
import com.example.minimall.service.CategoryService;
import com.example.minimall.service.CouponService;
import com.example.minimall.service.DiscountActivityService;
import com.example.minimall.service.ProductService;
import com.example.minimall.service.ProductSpecService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 商品服务内部 API — 供其他微服务通过 Feign 调用（不经网关，不走 JWT 认证）。
 *
 * <p>路径前缀：/api/internal/**，在 SecurityConfig 中白名单放行。
 * 返回裸对象（不包 Result），方便 Feign 客户端直接反序列化。
 */
@RestController
@RequestMapping("/api/internal")
public class InternalApiController {

    private static final Logger logger = LoggerFactory.getLogger(InternalApiController.class);

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final ProductSpecService productSpecService;
    private final ProductSpecMapper productSpecMapper;
    private final DiscountActivityService discountActivityService;
    private final CouponService couponService;

    public InternalApiController(ProductService productService,
                                  ProductMapper productMapper,
                                  CategoryService categoryService,
                                  ProductSpecService productSpecService,
                                  ProductSpecMapper productSpecMapper,
                                  DiscountActivityService discountActivityService,
                                  CouponService couponService) {
        this.productService = productService;
        this.productMapper = productMapper;
        this.categoryService = categoryService;
        this.productSpecService = productSpecService;
        this.productSpecMapper = productSpecMapper;
        this.discountActivityService = discountActivityService;
        this.couponService = couponService;
    }

    // ======================== 商品 ========================

    /** 根据 ID 查询商品（供 order-service / ai-service 调用） */
    @GetMapping("/product/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.findById(id);
    }

    /** 批量查询商品（供 order-service 订单列表补充商品信息） */
    @PostMapping("/product/batch")
    public List<Product> getProductsBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return productMapper.selectBatchIds(ids);
    }

    /** 关键字搜索商品（供 ai-service 商品推荐调用） */
    @GetMapping("/product/search")
    public List<Product> searchProducts(@RequestParam String keyword) {
        return productService.search(keyword);
    }

    /** 商品全量列表（供 ai-service 调用） */
    @GetMapping("/product/list")
    public List<Product> listProducts() {
        return productService.listAll();
    }

    /** 扣减/回滚库存（供 order-service 创建订单/取消订单调用）
     *  decrease > 0 扣减，decrease < 0 回滚；SQL 带乐观校验 stock >= decrease */
    @PutMapping("/product/{id}/stock")
    public boolean updateStock(@PathVariable Long id, @RequestParam Integer decrease) {
        logger.info("Internal: updateStock id={}, decrease={}", id, decrease);
        // oldStock 参数在 SQL 中未使用（SQL 用 stock >= decrease 做乐观校验），传 0 占位
        int rows = productMapper.updateStockById(id, 0, decrease);
        return rows > 0;
    }

    /** 查询商品规格（供 order-service 调用） */
    @GetMapping("/product/{id}/specs")
    public List<ProductSpec> getProductSpecs(@PathVariable Long id) {
        return productSpecService.findByProductId(id);
    }

    /** 按卖家（商家）ID 查询商品列表（供 order-service 商家订单查询调用） */
    @GetMapping("/product/list-by-seller")
    public List<Product> listProductsBySeller(@RequestParam Long sellerId) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("seller_id", sellerId);
        return productMapper.selectList(wrapper);
    }

    // ======================== 商品规格 ========================

    /** 根据规格 ID 查询单条规格（供 order-service 创建订单调用） */
    @GetMapping("/spec/{specId}")
    public ProductSpec getSpec(@PathVariable Long specId) {
        return productSpecService.findById(specId);
    }

    /** 扣减/回滚规格库存（供 order-service 创建订单/取消订单调用）
     *  decrease > 0 扣减，decrease < 0 回滚；SQL 带乐观校验 stock >= decrease */
    @PutMapping("/spec/{specId}/stock")
    public boolean updateSpecStock(@PathVariable Long specId, @RequestParam Integer decrease) {
        logger.info("Internal: updateSpecStock specId={}, decrease={}", specId, decrease);
        int rows = productSpecMapper.updateStockById(specId, 0, decrease);
        return rows > 0;
    }

    // ======================== 优惠券 ========================

    /** 计算用户优惠券对应订单金额的优惠额（供 order-service 创建订单调用） */
    @GetMapping("/coupon/calculate/{userCouponId}")
    public BigDecimal calculateCouponDiscount(@PathVariable Long userCouponId,
                                              @RequestParam BigDecimal amount) {
        return couponService.calculateDiscount(userCouponId, amount);
    }

    /** 核销用户优惠券（供 order-service 创建订单调用） */
    @PostMapping("/coupon/use/{userCouponId}")
    public boolean useCoupon(@PathVariable Long userCouponId) {
        couponService.useCoupon(userCouponId);
        return true;
    }

    // ======================== 分类 ========================

    /** 分类列表（供 ai-service 调用） */
    @GetMapping("/category/list")
    public List<Category> listCategories() {
        return categoryService.listAll();
    }

    /** 根据 ID 查询分类（供 ai-service 商品上下文构建调用） */
    @GetMapping("/category/{id}")
    public Category getCategory(@PathVariable Long id) {
        return categoryService.findById(id);
    }

    // ======================== 折扣活动 ========================

    /** 当前有效折扣活动（供 ai-service / order-service 调用） */
    @GetMapping("/discount/active")
    public List<DiscountActivity> getActiveDiscounts() {
        return discountActivityService.getActiveActivities();
    }
}
