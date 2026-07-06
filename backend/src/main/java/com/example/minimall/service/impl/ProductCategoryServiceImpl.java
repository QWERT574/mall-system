package com.example.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.minimall.mapper.ProductCategoryMapper;
import com.example.minimall.model.ProductCategory;
import com.example.minimall.service.ProductCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类服务实现类
 * 实现ProductCategoryService接口，提供具体的业务逻辑
 */
@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {
    
    /** 商品分类Mapper */
    private final ProductCategoryMapper mapper;
    
    public ProductCategoryServiceImpl(ProductCategoryMapper mapper) {
        this.mapper = mapper;
    }
    
    /**
     * 获取所有分类列表
     * @return 分类列表
     */
    @Override
    public List<ProductCategory> listAll() {
        QueryWrapper<ProductCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", 0).orderByAsc("sort");
        return mapper.selectList(queryWrapper);
    }
    
    /**
     * 根据ID获取分类
     * @param id 分类ID
     * @return 分类对象
     */
    @Override
    public ProductCategory findById(Long id) {
        return mapper.selectById(id);
    }
    
    /**
     * 获取顶级分类列表
     * @return 顶级分类列表
     */
    @Override
    public List<ProductCategory> listTopCategories() {
        QueryWrapper<ProductCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", 0).eq("is_deleted", 0).orderByAsc("sort");
        return mapper.selectList(queryWrapper);
    }
    
    /**
     * 根据父分类ID获取子分类
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @Override
    public List<ProductCategory> listByParentId(Long parentId) {
        QueryWrapper<ProductCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId).eq("is_deleted", 0).orderByAsc("sort");
        return mapper.selectList(queryWrapper);
    }
    
    /**
     * 保存分类
     * @param category 分类对象
     * @return 是否保存成功
     */
    @Override
    public boolean save(ProductCategory category) {
        if (category.getId() == null) {
            // 新增分类
            category.setIsDeleted(0);
            return mapper.insert(category) > 0;
        } else {
            // 更新分类
            return mapper.updateById(category) > 0;
        }
    }
    
    /**
     * 删除分类
     * @param id 分类ID
     */
    @Override
    public void delete(Long id) {
        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setIsDeleted(1); // 软删除
        mapper.updateById(category);
    }
    
    /**
     * 检查分类下是否有子分类
     * @param id 分类ID
     * @return 是否有子分类
     */
    @Override
    public boolean hasChildren(Long id) {
        QueryWrapper<ProductCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", id).eq("is_deleted", 0);
        return mapper.selectCount(queryWrapper) > 0;
    }
    
    /**
     * 检查分类下是否有商品
     * @param id 分类ID
     * @return 是否有商品
     */
    @Override
    public boolean hasProducts(Long id) {
        // 这里可以查询分类商品关联表，检查是否有商品
        // 目前返回false，实际项目中需要实现
        return false;
    }
}
