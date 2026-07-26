package com.example.minimall.feign;

import com.example.minimall.model.Category;
import com.example.minimall.model.DiscountActivity;
import com.example.minimall.model.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 商品服务 Feign 客户端 — 调用 product-service 的内部 API（/api/internal/**）。
 *
 * <p>用于替代原先直接访问 product / category / discount_activity 表的本地 Service。
 * 服务间直连，不经网关，由 Nacos 解析服务名 {@code product-service}。</p>
 *
 * <p><b>返回类型说明</b>：返回 ai-service 本地的 {@link Product} / {@link Category} /
 * {@link DiscountActivity} 模型（作为 DTO 使用），Jackson 按 字段名 匹配反序列化
 * product-service 返回的 JSON，多余字段自动忽略。这些模型在 ai-service 中不再有对应 Mapper，
 * 仅为承载 Feign 响应数据的纯 POJO。</p>
 */
@FeignClient(name = "product-service", contextId = "aiProductFeignClient")
public interface ProductFeignClient {

    // ======================== 商品 ========================

    /** 商品全量列表（替代原 ProductService.listAll） */
    @GetMapping("/api/internal/product/list")
    List<Product> listAll();

    /** 根据 ID 查询商品（替代原 ProductService.findById） */
    @GetMapping("/api/internal/product/{id}")
    Product getProduct(@PathVariable("id") Long id);

    /** 关键字搜索商品（替代原 ProductService.search） */
    @GetMapping("/api/internal/product/search")
    List<Product> searchProducts(@RequestParam("keyword") String keyword);

    // ======================== 分类 ========================

    /** 分类全量列表（替代原 CategoryService.listAll） */
    @GetMapping("/api/internal/category/list")
    List<Category> listCategories();

    /** 根据 ID 查询分类（替代原 CategoryService.findById） */
    @GetMapping("/api/internal/category/{id}")
    Category getCategory(@PathVariable("id") Long id);

    // ======================== 折扣活动 ========================

    /** 当前有效折扣活动（替代原 DiscountActivityService.getActiveActivities） */
    @GetMapping("/api/internal/discount/active")
    List<DiscountActivity> getActiveDiscounts();
}
