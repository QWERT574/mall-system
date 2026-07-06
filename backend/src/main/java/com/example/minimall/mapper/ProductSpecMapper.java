package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ProductSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品规格表 Mapper，对应 product_spec 表
 */
@Mapper
public interface ProductSpecMapper extends BaseMapper<ProductSpec> {
    /** 根据商品 ID 查询规格列表 */
    List<ProductSpec> selectByProductId(@Param("productId") Long productId);
    /** 乐观锁更新规格库存 */
    int updateStockById(@Param("id") Long id, @Param("oldStock") Integer oldStock, @Param("decrease") Integer decrease);
}