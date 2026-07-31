package com.example.minimall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.minimall.model.DiscountActivity;
import com.example.minimall.model.DiscountActivityProduct;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 优惠活动服务接口 */
public interface DiscountActivityService {

    /** 分页查询活动（**支持按 status 过滤**） */
    IPage<DiscountActivity> getActivities(int page, int size, Integer status);

    /** 分页查询进行中的活动（**按时间筛选**） */
    IPage<DiscountActivity> getActiveActivities(int page, int size);

    /** 获取所有进行中的活动（**不分页**） */
    List<DiscountActivity> getActiveActivities();

    /** 根据 ID 查询活动 */
    DiscountActivity findById(Long id);

    /** 获取活动下挂载的商品列表 */
    List<DiscountActivityProduct> getActivityProducts(Long activityId);

    /** 创建活动（**事务**） */
    DiscountActivity createActivity(DiscountActivity activity);

    /** 更新活动（**事务**） */
    DiscountActivity updateActivity(Long id, DiscountActivity activity);

    /** 删除活动（**事务**，含级联删除活动商品） */
    void deleteActivity(Long id);

    /** 添加活动商品（**事务**） */
    void addActivityProduct(Long activityId, Long productId, BigDecimal discountPrice);

    /** 移除活动商品（**事务**） */
    void removeActivityProduct(Long id);

    /**
     * 计算活动优惠金额
     * <p>对某商品在某活动下，计算 originalPrice → 实际价格的减免值</p>
     */
    BigDecimal calculateDiscount(Long activityId, Long productId, BigDecimal originalPrice);

    /**
     * 获取进行中的活动及其商品（**返回 Map 列表**，用于前端聚合展示）
     */
    List<Map<String, Object>> getActiveActivitiesWithProducts();
}
