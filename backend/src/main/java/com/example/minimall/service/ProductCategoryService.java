package com.example.minimall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.minimall.model.ProductCategory;

import java.util.List;

/**
 * 商品分类服务接口
 * 继承IService，提供更多的业务方法
 */
public interface ProductCategoryService extends IService<ProductCategory> {
    
    /**
     * 获取所有分类列表
     * @return 分类列表
     */
    List<ProductCategory> listAll();
    
    /**
     * 根据ID获取分类
     * @param id 分类ID
     * @return 分类对象
     */
    ProductCategory findById(Long id);
    
    /**
     * 获取顶级分类列表
     * @return 顶级分类列表
     */
    List<ProductCategory> listTopCategories();
    
    /**
     * 根据父分类ID获取子分类
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<ProductCategory> listByParentId(Long parentId);
    
    /**
     * 保存分类
     * @param category 分类对象
     * @return 是否保存成功
     */
    boolean save(ProductCategory category);
    
    /**
     * 删除分类
     * @param id 分类ID
     */
    void delete(Long id);
    
    /**
     * 检查分类下是否有子分类
     * @param id 分类ID
     * @return 是否有子分类
     */
    boolean hasChildren(Long id);
    
    /**
     * 检查分类下是否有商品
     * @param id 分类ID
     * @return 是否有商品
     */
    boolean hasProducts(Long id);
}
