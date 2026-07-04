package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品表 Mapper，对应 product 表
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    /** 根据关键字模糊查询商品 */
    List<Product> selectByKeyword(@Param("keyword") String keyword);
    /** 分页按关键字查询商品 */
    IPage<Product> selectPageByKeyword(Page<Product> page, @Param("keyword") String keyword);
    /** 根据标签 ID 查询关联商品 */
    List<Product> selectByTagId(@Param("tagId") Long tagId);
    /** 随机分页查询商品 */
    IPage<Product> selectRandomPage(Page<Product> page);
    /** 获取商品及其销量信息 */
    List<Product> selectProductWithSales();
    /** 乐观锁更新商品库存 */
    int updateStockById(@Param("id") Long id, @Param("oldStock") Integer oldStock, @Param("decrease") Integer decrease);
    /** 获取指定分类的统计信息 */
    List<java.util.Map<String, Object>> selectCategoryStatistics(@Param("categoryIds") List<Long> categoryIds);
    /** 获取指定分类的对比数据 */
    java.util.Map<String, Object> selectCategoryComparison(@Param("categoryIds") List<Long> categoryIds);
}