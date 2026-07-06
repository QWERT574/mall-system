package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.minimall.model.DiscountActivity;
import com.example.minimall.model.DiscountActivityProduct;
import com.example.minimall.service.DiscountActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 折扣活动（限时折扣/特价等）相关接口 */
@RestController
@RequestMapping("/api/discount")
public class DiscountActivityController {

    /** 折扣活动业务服务 */
    @Autowired
    private DiscountActivityService discountActivityService;

    /** 分页获取折扣活动列表，可按状态筛选 */
    @GetMapping("/list")
    public Map<String, Object> getActivities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        try {
            IPage<DiscountActivity> result = discountActivityService.getActivities(page, size, status);
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("total", result.getTotal());
            pageInfo.put("records", result.getRecords());
            pageInfo.put("current", result.getCurrent());
            pageInfo.put("size", result.getSize());
            pageInfo.put("pages", result.getPages());
            return createSuccessResponse(pageInfo);
        } catch (Exception e) {
            return createErrorResponse("获取活动列表失败: " + e.getMessage());
        }
    }

    /** 分页获取进行中的折扣活动 */
    @GetMapping("/active")
    public Map<String, Object> getActiveActivities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            IPage<DiscountActivity> result = discountActivityService.getActiveActivities(page, size);
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("total", result.getTotal());
            pageInfo.put("records", result.getRecords());
            pageInfo.put("current", result.getCurrent());
            pageInfo.put("size", result.getSize());
            pageInfo.put("pages", result.getPages());
            return createSuccessResponse(pageInfo);
        } catch (Exception e) {
            return createErrorResponse("获取进行中活动失败: " + e.getMessage());
        }
    }

    /** 根据 ID 获取折扣活动详情 */
    @GetMapping("/{id}")
    public Map<String, Object> getActivity(@PathVariable Long id) {
        try {
            DiscountActivity activity = discountActivityService.findById(id);
            if (activity == null) return createErrorResponse("活动不存在");
            return createSuccessResponse(activity);
        } catch (Exception e) {
            return createErrorResponse("获取活动失败: " + e.getMessage());
        }
    }

    /** 获取折扣活动下的所有商品 */
    @GetMapping("/{id}/products")
    public Map<String, Object> getActivityProducts(@PathVariable Long id) {
        try {
            List<DiscountActivityProduct> products = discountActivityService.getActivityProducts(id);
            return createSuccessResponse(products);
        } catch (Exception e) {
            return createErrorResponse("获取活动商品失败: " + e.getMessage());
        }
    }

    /** 获取进行中的折扣活动及其商品（一次性返回） */
    @GetMapping("/active-with-products")
    public Map<String, Object> getActiveActivitiesWithProducts() {
        try {
            List<Map<String, Object>> result = discountActivityService.getActiveActivitiesWithProducts();
            return createSuccessResponse(result);
        } catch (Exception e) {
            return createErrorResponse("获取活动失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Map<String, Object> createActivity(@RequestBody DiscountActivity activity) {
        try {
            DiscountActivity created = discountActivityService.createActivity(activity);
            return createSuccessResponse(created);
        } catch (Exception e) {
            return createErrorResponse("创建活动失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateActivity(@PathVariable Long id, @RequestBody DiscountActivity activity) {
        try {
            DiscountActivity updated = discountActivityService.updateActivity(id, activity);
            return createSuccessResponse(updated);
        } catch (Exception e) {
            return createErrorResponse("更新活动失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteActivity(@PathVariable Long id) {
        try {
            discountActivityService.deleteActivity(id);
            return createSuccessResponse("删除成功");
        } catch (Exception e) {
            return createErrorResponse("删除活动失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/product")
    public Map<String, Object> addActivityProduct(
            @PathVariable Long id,
            @RequestParam Long productId,
            @RequestParam BigDecimal discountPrice) {
        try {
            discountActivityService.addActivityProduct(id, productId, discountPrice);
            return createSuccessResponse("添加成功");
        } catch (Exception e) {
            return createErrorResponse("添加活动商品失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/product/{id}")
    public Map<String, Object> removeActivityProduct(@PathVariable Long id) {
        try {
            discountActivityService.removeActivityProduct(id);
            return createSuccessResponse("移除成功");
        } catch (Exception e) {
            return createErrorResponse("移除活动商品失败: " + e.getMessage());
        }
    }

    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 1);
        response.put("message", message);
        return response;
    }
}
