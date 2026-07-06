package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.DiscountActivityProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 折扣活动商品关联表 Mapper，对应 discount_activity_product 表
 */
@Mapper
public interface DiscountActivityProductMapper extends BaseMapper<DiscountActivityProduct> {
    /** 根据活动 ID 查询参与活动的商品列表 */
    List<DiscountActivityProduct> selectByActivityId(@Param("activityId") Long activityId);
}
