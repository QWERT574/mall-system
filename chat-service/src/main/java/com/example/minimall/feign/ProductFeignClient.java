package com.example.minimall.feign;

import com.example.minimall.model.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务 Feign 客户端 — 调用 product-service 的内部 API（/api/internal/**）。
 *
 * <p>用于替代原本地 {@code ProductMapper} 的跨域直访。</p>
 *
 * <p>主要使用场景：
 * {@link com.example.minimall.service.impl.AdminInterventionServiceImpl}
 * 在 enrich 介入申请详情时，根据 productId 查询商品名称。</p>
 */
@FeignClient(name = "product-service", contextId = "productFeignClient")
public interface ProductFeignClient {

    /** 根据 ID 查询商品 */
    @GetMapping("/api/internal/product/{id}")
    Product getProduct(@PathVariable("id") Long id);
}
