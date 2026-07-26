package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ProductTagRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品标签关联表 Mapper，对应 product_tag_relation 表
 */
@Mapper
public interface ProductTagRelationMapper extends BaseMapper<ProductTagRelation> {
}
