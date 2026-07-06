package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ProductTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品标签表 Mapper，对应 product_tag 表
 */
@Mapper
public interface ProductTagMapper extends BaseMapper<ProductTag> {
    /** 根据商品 ID 查询关联的标签列表 */
    List<ProductTag> selectByProductId(@Param("productId") Long productId);
}
