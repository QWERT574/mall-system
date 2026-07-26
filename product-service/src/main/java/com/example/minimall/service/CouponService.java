package com.example.minimall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.minimall.model.Coupon;
import com.example.minimall.model.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

/** 优惠券服务接口 */
public interface CouponService {

    /** 分页查询可领取的优惠券（**有效期内 + 库存 > 0**） */
    IPage<Coupon> getAvailableCoupons(int page, int size);

    /** 根据 ID 查询优惠券 */
    Coupon findById(Long id);

    /**
     * 分页查询商家优惠券（**商家后台**）
     * <p>支持按 status 过滤</p>
     */
    IPage<Coupon> getSellerCoupons(Long sellerId, int page, int size, Integer status);

    /** 创建优惠券（**事务**） */
    Coupon createCoupon(Coupon coupon);

    /** 更新优惠券（**事务**） */
    Coupon updateCoupon(Long id, Coupon coupon);

    /** 删除优惠券（**事务**） */
    void deleteCoupon(Long id);

    /**
     * 领取优惠券（**事务**）
     * <p>校验库存、有效期、是否已领取</p>
     */
    UserCoupon claimCoupon(Long couponId, Long userId);

    /** 获取用户的全部优惠券（含已使用/已过期） */
    List<UserCoupon> getUserCoupons(Long userId);

    /** 根据 ID 获取用户优惠券 */
    UserCoupon getUserCouponById(Long id);

    /** 核销用户优惠券（**事务**，置为已使用） */
    void useCoupon(Long userCouponId);

    /**
     * 计算订单优惠金额
     * <p>根据优惠券类型（满减 / 折扣 / 无门槛）计算实际减免</p>
     */
    BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderTotal);
}
