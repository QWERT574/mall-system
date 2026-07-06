package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类表 Mapper，对应 category 表
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}