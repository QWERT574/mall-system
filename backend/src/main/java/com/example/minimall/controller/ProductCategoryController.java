package com.example.minimall.controller;

import com.example.minimall.model.ProductCategory;
import com.example.minimall.service.ProductCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品分类控制器
 * 提供商品分类的API接口
 */
@RestController
@RequestMapping("/api/product-category")
public class ProductCategoryController {
    /** 商品分类业务服务 */
    private final ProductCategoryService service;
    
    public ProductCategoryController(ProductCategoryService service) {
        this.service = service;
    }
    
    // 创建成功响应
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

    // 获取所有分类列表
    /**
     * 获取全部分类列表（平铺结构）
     */
    @GetMapping("/list")
    public Map<String, Object> list() {
        List<ProductCategory> categories = service.listAll();
        return createSuccessResponse(categories);
    }
    
    // 获取顶级分类列表
    /**
     * 获取所有顶级（一级）分类
     */
    @GetMapping("/top")
    public Map<String, Object> top() {
        List<ProductCategory> categories = service.listTopCategories();
        return createSuccessResponse(categories);
    }
    
    // 根据父分类ID获取子分类
    /**
     * 根据父分类 ID 获取其下子分类列表
     */
    @GetMapping("/children")
    public Map<String, Object> children(Long parentId) {
        List<ProductCategory> categories = service.listByParentId(parentId);
        return createSuccessResponse(categories);
    }
    
    // 根据ID获取分类
    /**
     * 根据 ID 获取单个分类详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        ProductCategory category = service.findById(id);
        return createSuccessResponse(category);
    }
    
    // 创建分类
    /**
     * 创建一个商品分类
     */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody ProductCategory category) {
        try {
            service.save(category);
            return createSuccessResponse(category);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("创建分类失败: " + e.getMessage());
        }
    }
    
    // 更新分类
    /**
     * 更新指定 ID 的商品分类
     */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody ProductCategory category) {
        try {
            category.setId(id);
            service.save(category);
            return createSuccessResponse(category);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新分类失败: " + e.getMessage());
        }
    }
    
    // 删除分类
    /**
     * 删除指定分类（存在子分类或商品时不允许删除）
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        try {
            // 检查分类下是否有子分类
            if (service.hasChildren(id)) {
                return createErrorResponse("该分类下存在子分类，无法删除");
            }
            // 检查分类下是否有商品
            if (service.hasProducts(id)) {
                return createErrorResponse("该分类下存在商品，无法删除");
            }
            service.delete(id);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("删除分类失败: " + e.getMessage());
        }
    }
}
