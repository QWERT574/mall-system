package com.example.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.enums.DiscountTypeEnum;
import com.example.minimall.mapper.DiscountActivityMapper;
import com.example.minimall.mapper.DiscountActivityProductMapper;
import com.example.minimall.mapper.ProductMapper;
import com.example.minimall.model.DiscountActivity;
import com.example.minimall.model.DiscountActivityProduct;
import com.example.minimall.model.Product;
import com.example.minimall.service.DiscountActivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 优惠活动服务实现 */
@Service
/**
 * 折扣活动业务实现
 * <p>
 * 负责折扣活动的 CRUD、关联商品、折扣计算等核心业务。
 * 与 CouponService 的区别：本类是"全场折扣/限时折扣"（绑定商品），优惠券是"用户领券"。
 * </p>
 */
public class DiscountActivityServiceImpl implements DiscountActivityService {

    /** 活动Mapper */
    private final DiscountActivityMapper activityMapper;
    /** 活动商品关联Mapper */
    private final DiscountActivityProductMapper productMapper;
    /** 商品Mapper */
    private final ProductMapper pMapper;

    public DiscountActivityServiceImpl(DiscountActivityMapper activityMapper,
                                   DiscountActivityProductMapper productMapper,
                                   ProductMapper pMapper) {
        this.activityMapper = activityMapper;
        this.productMapper = productMapper;
        this.pMapper = pMapper;
    }

    @Override
    public IPage<DiscountActivity> getActivities(int page, int size, Integer status) {
        Page<DiscountActivity> pageQuery = new Page<>(page, size);
        QueryWrapper<DiscountActivity> wrapper = new QueryWrapper<>();
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("id");
        return activityMapper.selectPage(pageQuery, wrapper);
    }

    @Override
    /** 分页查询当前生效中的活动 */
    public IPage<DiscountActivity> getActiveActivities(int page, int size) {
        Page<DiscountActivity> pageQuery = new Page<>(page, size);
        QueryWrapper<DiscountActivity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .le("start_time", LocalDateTime.now())
               .orderByDesc("id");
        return activityMapper.selectPage(pageQuery, wrapper);
    }

    @Override
    public List<DiscountActivity> getActiveActivities() {
        QueryWrapper<DiscountActivity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .le("start_time", LocalDateTime.now())
               .orderByDesc("id");
        return activityMapper.selectList(wrapper);
    }

    @Override
    /** 根据 ID 查询活动 */
    public DiscountActivity findById(Long id) {
        return activityMapper.selectById(id);
    }

    @Override
    /** 获取活动下的商品 */
    public List<DiscountActivityProduct> getActivityProducts(Long activityId) {
        return productMapper.selectByActivityId(activityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /** 创建优惠活动 */
    public DiscountActivity createActivity(DiscountActivity activity) {
        if (activity.getStatus() == null) activity.setStatus(1);
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.insert(activity);
        return activity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /** 更新活动 */
    public DiscountActivity updateActivity(Long id, DiscountActivity activity) {
        DiscountActivity existing = activityMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("活动不存在");
        if (activity.getName() != null) existing.setName(activity.getName());
        if (activity.getType() != null) existing.setType(activity.getType());
        if (activity.getDiscountRate() != null) existing.setDiscountRate(activity.getDiscountRate());
        if (activity.getThreshold() != null) existing.setThreshold(activity.getThreshold());
        if (activity.getReduceAmount() != null) existing.setReduceAmount(activity.getReduceAmount());
        if (activity.getStartTime() != null) existing.setStartTime(activity.getStartTime());
        if (activity.getEndTime() != null) existing.setEndTime(activity.getEndTime());
        if (activity.getStatus() != null) existing.setStatus(activity.getStatus());
        if (activity.getDescription() != null) existing.setDescription(activity.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long id) {
        QueryWrapper<DiscountActivityProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("activity_id", id);
        productMapper.delete(wrapper);
        activityMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /** 添加活动商品 */
    public void addActivityProduct(Long activityId, Long productId, BigDecimal discountPrice) {
        DiscountActivity activity = activityMapper.selectById(activityId);
        if (activity == null) throw new IllegalArgumentException("活动不存在");
        Product product = pMapper.selectById(productId);
        if (product == null) throw new IllegalArgumentException("商品不存在");

        DiscountActivityProduct dap = new DiscountActivityProduct();
        dap.setActivityId(activityId);
        dap.setProductId(productId);
        dap.setDiscountPrice(discountPrice);
        productMapper.insert(dap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /** 移除活动商品 */
    public void removeActivityProduct(Long id) {
        productMapper.deleteById(id);
    }

    @Override
    /** 计算活动优惠金额 */
    public BigDecimal calculateDiscount(Long activityId, Long productId, BigDecimal originalPrice) {
        DiscountActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !activity.getStatus().equals(1)) return BigDecimal.ZERO;

        QueryWrapper<DiscountActivityProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("activity_id", activityId).eq("product_id", productId);
        DiscountActivityProduct dap = productMapper.selectOne(wrapper);
        if (dap == null) return BigDecimal.ZERO;

        if (activity.getType().equals(DiscountTypeEnum.FLASH_SALE.getCode())) {
            return originalPrice.subtract(dap.getDiscountPrice());
        } else if (activity.getType().equals(DiscountTypeEnum.DISCOUNT.getCode())) {
            BigDecimal rate = activity.getDiscountRate();
            if (rate == null) return BigDecimal.ZERO;
            BigDecimal factor = rate.divide(BigDecimal.TEN, 2, java.math.RoundingMode.HALF_UP);
            return originalPrice.subtract(originalPrice.multiply(factor));
        }
        return BigDecimal.ZERO;
    }

    @Override
    /** 获取生效活动及其关联商品 */
    /**
     * 获取进行中的活动及其商品（首页「优惠活动」用）
     *
     * @return 活动列表，每个活动含商品列表字段
     */
    public List<Map<String, Object>> getActiveActivitiesWithProducts() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        QueryWrapper<DiscountActivity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .le("start_time", LocalDateTime.now())
               .orderByDesc("id");
        List<DiscountActivity> activities = activityMapper.selectList(wrapper);
        for (DiscountActivity activity : activities) {
            Map<String, Object> activityMap = new java.util.HashMap<>();
            activityMap.put("id", activity.getId());
            activityMap.put("name", activity.getName());
            activityMap.put("type", activity.getType());
            activityMap.put("discountRate", activity.getDiscountRate());
            activityMap.put("threshold", activity.getThreshold());
            activityMap.put("reduceAmount", activity.getReduceAmount());
            activityMap.put("startTime", activity.getStartTime());
            activityMap.put("endTime", activity.getEndTime());
            activityMap.put("status", activity.getStatus());
            activityMap.put("description", activity.getDescription());
            List<DiscountActivityProduct> products = productMapper.selectByActivityId(activity.getId());
            activityMap.put("products", products);
            result.add(activityMap);
        }
        return result;
    }
}
