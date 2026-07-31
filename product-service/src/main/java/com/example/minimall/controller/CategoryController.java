package com.example.minimall.controller;

import com.example.minimall.model.Category;
import com.example.minimall.service.CategoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 商品分类管理接口（支持缓存与清除策略） */
@RestController
@RequestMapping("/api/category")
public class CategoryController {
    /** 分类业务服务 */
    private final CategoryService service;
    
    public CategoryController(CategoryService service) {
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

    /** 获取所有分类列表（按名称去重） */
    @GetMapping("/list")
    public Map<String, Object> list() {
        List<Category> categories = service.listAll();
        // 去重处理，只返回唯一的分类名称
        List<Category> uniqueCategories = categories.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Category::getName, // 以名称为key
                        category -> category, // 以分类对象为value
                        (existing, replacement) -> existing) // 如果名称重复，保留第一个
                )
                .values()
                .stream()
                .collect(java.util.stream.Collectors.toList());
        return createSuccessResponse(uniqueCategories);
    }
    
    /** 获取顶级分类列表 */
    @GetMapping("/top")
    public Map<String, Object> top() {
        List<Category> categories = service.listTopCategories();
        return createSuccessResponse(categories);
    }
    
    /** 根据父分类 ID 获取子分类（不使用缓存） */
    @GetMapping("/children")
    public Map<String, Object> children(Long parentId) {
        List<Category> categories = service.listByParentId(parentId);
        return createSuccessResponse(categories);
    }
    
    /** 根据 ID 获取分类详情（带缓存） */
    @Cacheable(value = "category:detail", key = "#id", unless = "#result == null")
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Category category = service.findById(id);
        return createSuccessResponse(category);
    }
    
    /** 创建分类并清除相关缓存 */
    @CacheEvict(value = {"category:list", "category:top", "category:children", "category:detail"}, allEntries = true)
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Category category) {
        try {
            service.save(category);
            return createSuccessResponse(category);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("创建分类失败: " + e.getMessage());
        }
    }
    
    /** 更新分类并清除相关缓存 */
    @CacheEvict(value = {"category:list", "category:top", "category:children", "category:detail"}, allEntries = true)
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Category category) {
        try {
            category.setId(id);
            service.save(category);
            return createSuccessResponse(category);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新分类失败: " + e.getMessage());
        }
    }
    
    /** 删除分类（存在子分类或商品时禁止删除）并清除缓存 */
    @CacheEvict(value = {"category:list", "category:top", "category:children", "category:detail"}, allEntries = true)
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
    
    /** 批量删除分类并清除相关缓存 */
    @CacheEvict(value = {"category:list", "category:top", "category:children", "category:detail"}, allEntries = true)
    @PostMapping("/batch-delete")
    public Map<String, Object> batchDelete(@RequestBody List<Long> ids) {
        try {
            service.batchDelete(ids);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("批量删除分类失败: " + e.getMessage());
        }
    }
    
    /** 更新分类启用/禁用状态 */
    @PostMapping("/{id}/update-status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            Integer status = request.get("status");
            service.updateStatus(id, status);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新分类状态失败: " + e.getMessage());
        }
    }
    
    /** 更新分类排序 */
    @PostMapping("/{id}/update-sort")
    public Map<String, Object> updateSort(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            Integer sort = request.get("sort");
            service.updateSort(id, sort);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新分类排序失败: " + e.getMessage());
        }
    }
}