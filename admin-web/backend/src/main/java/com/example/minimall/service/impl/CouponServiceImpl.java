package com.example.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.enums.CouponStatusEnum;
import com.example.minimall.enums.CouponTypeEnum;
import com.example.minimall.enums.UserCouponStatusEnum;
import com.example.minimall.mapper.CouponMapper;
import com.example.minimall.mapper.UserCouponMapper;
import com.example.minimall.model.Coupon;
import com.example.minimall.model.UserCoupon;
import com.example.minimall.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券业务实现
 * <p>
 * 负责优惠券 CRUD、用户领取、核销、折扣计算等核心业务。
 * 关键设计：通过 CouponTypeEnum + UserCouponStatusEnum 枚举管理业务状态。
 * </p>
 */
@Service
public class CouponServiceImpl implements CouponService {

    /** 优惠券Mapper */
    private final CouponMapper couponMapper;
    /** 用户优惠券Mapper */
    private final UserCouponMapper userCouponMapper;

    public CouponServiceImpl(CouponMapper couponMapper, UserCouponMapper userCouponMapper) {
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    public IPage<Coupon> getAvailableCoupons(int page, int size) {
        Page<Coupon> pageQuery = new Page<>(page, size);
        QueryWrapper<Coupon> wrapper = new QueryWrapper<>();
        wrapper.eq("status", CouponStatusEnum.ACTIVE.getCode())
               .le("start_time", LocalDateTime.now())
               .ge("end_time", LocalDateTime.now())
               .gt("total_count", new QueryWrapper<Coupon>().apply("used_count").getSqlSegment().isEmpty() ? true : false)
               .orderByDesc("id");
        
        IPage<Coupon> result = couponMapper.selectPage(pageQuery, wrapper);
        for (Coupon c : result.getRecords()) {
            if (c.getUsedCount() == null) c.setUsedCount(0);
            if (c.getTotalCount() == null) c.setTotalCount(0);
        }
        return result;
    }

    @Override
    /** 根据 ID 查询优惠券 */
    public Coupon findById(Long id) {
        return couponMapper.selectById(id);
    }

    @Override
    /** 分页查询商家优惠券 */
    public IPage<Coupon> getSellerCoupons(Long sellerId, int page, int size, Integer status) {
        Page<Coupon> pageQuery = new Page<>(page, size);
        QueryWrapper<Coupon> wrapper = new QueryWrapper<>();
        if (sellerId != null) wrapper.eq("seller_id", sellerId);
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("id");
        return couponMapper.selectPage(pageQuery, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 创建优惠券
     * <p>
     * 自动填充默认值：totalCount=0、usedCount=0、perUserLimit=1、status=ACTIVE、threshold=0
     * </p>
     *
     * @param coupon 优惠券实体
     * @return 创建后的优惠券（含 ID）
     */
    public Coupon createCoupon(Coupon coupon) {
        if (coupon.getTotalCount() == null) coupon.setTotalCount(0);
        if (coupon.getUsedCount() == null) coupon.setUsedCount(0);
        if (coupon.getPerUserLimit() == null) coupon.setPerUserLimit(1);
        if (coupon.getStatus() == null) coupon.setStatus(CouponStatusEnum.ACTIVE.getCode());
        if (coupon.getThreshold() == null) coupon.setThreshold(BigDecimal.ZERO);
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        couponMapper.insert(coupon);
        return coupon;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新优惠券（部分字段，非全量替换）
     *
     * @param id     优惠券 ID
     * @param coupon 待更新字段
     * @return 更新后的优惠券
     */
    public Coupon updateCoupon(Long id, Coupon coupon) {
        Coupon existing = couponMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("优惠券不存在");
        if (coupon.getName() != null) existing.setName(coupon.getName());
        if (coupon.getType() != null) existing.setType(coupon.getType());
        if (coupon.getThreshold() != null) existing.setThreshold(coupon.getThreshold());
        if (coupon.getDiscountValue() != null) existing.setDiscountValue(coupon.getDiscountValue());
        if (coupon.getTotalCount() != null) existing.setTotalCount(coupon.getTotalCount());
        if (coupon.getPerUserLimit() != null) existing.setPerUserLimit(coupon.getPerUserLimit());
        if (coupon.getStartTime() != null) existing.setStartTime(coupon.getStartTime());
        if (coupon.getEndTime() != null) existing.setEndTime(coupon.getEndTime());
        if (coupon.getStatus() != null) existing.setStatus(coupon.getStatus());
        if (coupon.getDescription() != null) existing.setDescription(coupon.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());
        couponMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCoupon(Long id) {
        couponMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon claimCoupon(Long couponId, Long userId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) throw new IllegalArgumentException("优惠券不存在");
        if (!coupon.getStatus().equals(CouponStatusEnum.ACTIVE.getCode()))
            throw new IllegalArgumentException("优惠券已停用");
        if (coupon.getStartTime() != null && coupon.getStartTime().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("优惠券活动尚未开始");
        if (coupon.getEndTime() != null && coupon.getEndTime().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("优惠券已过期");
        if (coupon.getUsedCount() != null && coupon.getTotalCount() != null
                && coupon.getUsedCount() >= coupon.getTotalCount())
            throw new IllegalArgumentException("优惠券已领完");

        QueryWrapper<UserCoupon> userWrapper = new QueryWrapper<>();
        userWrapper.eq("user_id", userId).eq("coupon_id", couponId);
        long userClaimed = userCouponMapper.selectCount(userWrapper);
        if (userClaimed >= coupon.getPerUserLimit())
            throw new IllegalArgumentException("你已达到该优惠券领取上限");

        coupon.setUsedCount((coupon.getUsedCount() != null ? coupon.getUsedCount() : 0) + 1);
        couponMapper.updateById(coupon);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(UserCouponStatusEnum.UNUSED.getCode());
        userCoupon.setCreatedAt(LocalDateTime.now());
        userCouponMapper.insert(userCoupon);
        return userCoupon;
    }

    @Override
    public List<UserCoupon> getUserCoupons(Long userId) {
        return userCouponMapper.selectByUserIdWithCoupon(userId);
    }

    @Override
    public UserCoupon getUserCouponById(Long id) {
        return userCouponMapper.selectByIdWithCoupon(id);
    }

    /**
     * 标记用户优惠券为已使用（下单成功后调用）
     *
     * @param userCouponId 用户优惠券 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(Long userCouponId) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) throw new IllegalArgumentException("用户优惠券不存在");
        if (!uc.getStatus().equals(UserCouponStatusEnum.UNUSED.getCode()))
            throw new IllegalArgumentException("优惠券已使用或已过期");
        uc.setStatus(UserCouponStatusEnum.USED.getCode());
        uc.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(uc);
    }

    @Override
    /**
     * 计算订单使用某张优惠券可减免的金额
     * <p>
     * 按 CouponType 分支计算：
     * <ul>
     *   <li>满减券：orderTotal ≥ threshold 时减 discountAmount</li>
     *   <li>折扣券：orderTotal × discountRate</li>
     *   <li>无门槛：直接减 discountAmount</li>
     * </ul>
     * </p>
     *
     * @param userCouponId 用户优惠券 ID
     * @param orderTotal   订单金额
     * @return 减免金额（不满足条件返回 ZERO）
     */
    public BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderTotal) {
        UserCoupon uc = userCouponMapper.selectByIdWithCoupon(userCouponId);
        if (uc == null) return BigDecimal.ZERO;
        if (uc.getCouponType() == null) return BigDecimal.ZERO;

        if (uc.getCouponType().equals(CouponTypeEnum.FULL_REDUCTION.getCode())) {
            BigDecimal threshold = uc.getThreshold() != null ? uc.getThreshold() : BigDecimal.ZERO;
            if (orderTotal.compareTo(threshold) >= 0) {
                return uc.getDiscountValue();
            }
            return BigDecimal.ZERO;
        } else if (uc.getCouponType().equals(CouponTypeEnum.DISCOUNT.getCode())) {
            BigDecimal rate = uc.getDiscountValue() != null ? uc.getDiscountValue() : BigDecimal.TEN;
            BigDecimal factor = rate.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            BigDecimal discount = orderTotal.subtract(orderTotal.multiply(factor));
            return discount.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
