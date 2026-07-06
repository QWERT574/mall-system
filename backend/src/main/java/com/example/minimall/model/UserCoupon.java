package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户优惠券实体，对应 user_coupon 表，存储用户领取的优惠券及使用状态
 */
@TableName("user_coupon")
public class UserCoupon {
    @TableId
    private Long id;
    private Long userId;
    private Long couponId;
    private Integer status;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private Coupon coupon;

    @TableField(exist = false)
    private String couponName;

    @TableField(exist = false)
    private Integer couponType;

    @TableField(exist = false)
    private java.math.BigDecimal threshold;

    @TableField(exist = false)
    private java.math.BigDecimal discountValue;

    @TableField(exist = false)
    private LocalDateTime endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Coupon getCoupon() { return coupon; }
    public void setCoupon(Coupon coupon) { this.coupon = coupon; }
    public String getCouponName() { return couponName; }
    public void setCouponName(String couponName) { this.couponName = couponName; }
    public Integer getCouponType() { return couponType; }
    public void setCouponType(Integer couponType) { this.couponType = couponType; }
    public java.math.BigDecimal getThreshold() { return threshold; }
    public void setThreshold(java.math.BigDecimal threshold) { this.threshold = threshold; }
    public java.math.BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(java.math.BigDecimal discountValue) { this.discountValue = discountValue; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
