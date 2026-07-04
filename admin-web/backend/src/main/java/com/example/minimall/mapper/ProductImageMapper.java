package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ProductImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品图片表 Mapper，对应 product_image 表
 */
@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImage> {
}
