package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.DiscountActivity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 限时折扣活动表 Mapper，对应 discount_activity 表
 */
@Mapper
public interface DiscountActivityMapper extends BaseMapper<DiscountActivity> {
}
