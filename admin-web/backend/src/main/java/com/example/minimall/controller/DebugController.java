package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.CategoryMapper;
import com.example.minimall.mapper.ProductMapper;
import com.example.minimall.model.Category;
import com.example.minimall.model.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调试控制器，用于检查分类和商品数据
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {
    /** 分类 Mapper（调试用） */
    private final CategoryMapper categoryMapper;
    /** 商品 Mapper（调试用） */
    private final ProductMapper productMapper;

    public DebugController(CategoryMapper categoryMapper, ProductMapper productMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
    }

    /**
     * 获取所有分类和商品数据，用于调试分类问题
     * @return 分类和商品数据
     */
    @GetMapping("/category-product-data")
    public Map<String, Object> getCategoryProductData() {
        Map<String, Object> result = new HashMap<>();

        // 获取所有分类
        List<Category> categories = categoryMapper.selectList(null);
        result.put("categories", categories);

        // 获取所有商品
        List<Product> products = productMapper.selectList(null);
        result.put("products", products);

        // 统计每个分类下的商品数量
        Map<Long, Integer> categoryProductCount = new HashMap<>();
        products.forEach(product -> {
            Long categoryId = product.getCategoryId();
            categoryProductCount.put(categoryId, categoryProductCount.getOrDefault(categoryId, 0) + 1);
        });
        result.put("categoryProductCount", categoryProductCount);

        // 获取顶级分类
        QueryWrapper<Category> topQueryWrapper = new QueryWrapper<>();
        topQueryWrapper.eq("parent_id", 0).eq("is_deleted", 0);
        List<Category> topCategories = categoryMapper.selectList(topQueryWrapper);
        result.put("topCategories", topCategories);

        // 获取子分类
        QueryWrapper<Category> subQueryWrapper = new QueryWrapper<>();
        subQueryWrapper.ne("parent_id", 0).eq("is_deleted", 0);
        List<Category> subCategories = categoryMapper.selectList(subQueryWrapper);
        result.put("subCategories", subCategories);

        return result;
    }

    /**
     * 测试分类去重功能
     * @return 去重后的分类列表
     */
    @GetMapping("/test-category-deduplication")
    public List<Category> testCategoryDeduplication() {
        List<Category> categories = categoryMapper.selectList(null);
        // 去重处理，只返回唯一的分类名称
        return categories.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Category::getName, // 以名称为key
                        category -> category, // 以分类对象为value
                        (existing, replacement) -> existing) // 如果名称重复，保留第一个
                )
                .values()
                .stream()
                .collect(java.util.stream.Collectors.toList());
    }
}
