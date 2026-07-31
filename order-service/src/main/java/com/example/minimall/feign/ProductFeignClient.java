package com.example.minimall.feign;

import com.example.minimall.model.Product;
import com.example.minimall.model.ProductSpec;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品服务 Feign 客户端 — 调用 product-service 的内部 API（/api/internal/**）。
 *
 * <p>用于替代原先直接访问 product / product_spec / coupon 表的本地 Mapper。
 * 服务间直连，不经网关，由 Nacos 解析服务名 {@code product-service}。</p>
 */
@FeignClient(name = "product-service", contextId = "productFeignClient")
public interface ProductFeignClient {

    // ======================== 商品 ========================

    /** 根据 ID 查询商品 */
    @GetMapping("/api/internal/product/{id}")
    Product getProduct(@PathVariable("id") Long id);

    /** 批量查询商品 */
    @PostMapping("/api/internal/product/batch")
    List<Product> getProductsBatch(@RequestBody List<Long> ids);

    /** 扣减/回滚商品库存（decrease > 0 扣减，< 0 回滚） */
    @PutMapping("/api/internal/product/{id}/stock")
    boolean updateStock(@PathVariable("id") Long id, @RequestParam("decrease") Integer decrease);

    /** 按卖家 ID 查询商品列表 */
    @GetMapping("/api/internal/product/list-by-seller")
    List<Product> listProductsBySeller(@RequestParam("sellerId") Long sellerId);

    // ======================== 商品规格 ========================

    /** 根据规格 ID 查询规格 */
    @GetMapping("/api/internal/spec/{specId}")
    ProductSpec getSpec(@PathVariable("specId") Long specId);

    /** 扣减/回滚规格库存（decrease > 0 扣减，< 0 回滚） */
    @PutMapping("/api/internal/spec/{specId}/stock")
    boolean updateSpecStock(@PathVariable("specId") Long specId, @RequestParam("decrease") Integer decrease);

    // ======================== 优惠券 ========================

    /** 计算用户优惠券优惠额 */
    @GetMapping("/api/internal/coupon/calculate/{userCouponId}")
    BigDecimal calculateCouponDiscount(@PathVariable("userCouponId") Long userCouponId,
                                       @RequestParam("amount") BigDecimal amount);

    /** 核销用户优惠券 */
    @PostMapping("/api/internal/coupon/use/{userCouponId}")
    boolean useCoupon(@PathVariable("userCouponId") Long userCouponId);
}
