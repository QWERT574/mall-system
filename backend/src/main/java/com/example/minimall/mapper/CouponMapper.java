package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Coupon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券模板表 Mapper，对应 coupon 表
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
}
