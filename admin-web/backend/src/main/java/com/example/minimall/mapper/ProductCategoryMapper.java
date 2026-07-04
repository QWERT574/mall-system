package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品与分类关联表 Mapper，对应 product_category 表
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
