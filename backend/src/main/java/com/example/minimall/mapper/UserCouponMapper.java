package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户优惠券表 Mapper，对应 user_coupon 表
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
    /** 查询用户领取的优惠券（关联优惠券模板） */
    List<UserCoupon> selectByUserIdWithCoupon(@Param("userId") Long userId);
    /** 查询某张用户优惠券详情（关联优惠券模板） */
    UserCoupon selectByIdWithCoupon(@Param("id") Long id);
}
