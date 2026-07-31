package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.CategoryMapper;
import com.example.minimall.mapper.ProductMapper;
import com.example.minimall.model.Category;
import org.springframework.stereotype.Service;
import java.util.List;

/** 商品分类服务 */
@Service
public class CategoryService {
    /** 分类 Mapper */
    private final CategoryMapper mapper;
    /** 商品 Mapper（用于 hasProducts 查询） */
    private final ProductMapper productMapper;

    public CategoryService(CategoryMapper mapper, ProductMapper productMapper) {
        this.mapper = mapper;
        this.productMapper = productMapper;
    }
    
    /** 获取所有未删除的分类，按 sort 升序 */
    public List<Category> listAll() {
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", 0).orderByAsc("sort");
        return mapper.selectList(queryWrapper);
    }

    /**
     * 根据 ID 获取分类（含已软删除的）
     *
     * @param id 分类主键
     * @return 分类实体，未找到返回 null
     */
    public Category findById(Long id) {
        return mapper.selectById(id);
    }

    /** 获取顶级分类列表（parent_id = 0），按 sort 升序 */
    public List<Category> listTopCategories() {
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", 0).eq("is_deleted", 0).orderByAsc("sort");
        return mapper.selectList(queryWrapper);
    }

    /**
     * 根据父分类 ID 获取子分类列表
     *
     * @param parentId 父分类 ID（顶级分类时传 0）
     * @return 子分类列表
     */
    public List<Category> listByParentId(Long parentId) {
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId).eq("is_deleted", 0).orderByAsc("sort");
        return mapper.selectList(queryWrapper);
    }

    /**
     * 新增或更新分类（**根据 ID 是否为空判断**）
     *
     * @param category 分类实体（ID 为空时新增，否则更新）
     */
    public void save(Category category) {
        if (category.getId() == null) {
            // 新增分类，默认未删除
            category.setIsDeleted(0);
            mapper.insert(category);
        } else {
            // 更新分类
            mapper.updateById(category);
        }
    }

    /**
     * 软删除分类（**通过 is_deleted 标记**，不真删）
     *
     * @param id 分类主键
     */
    public void delete(Long id) {
        Category category = new Category();
        category.setId(id);
        category.setIsDeleted(1);
        mapper.updateById(category);
    }

    /**
     * 批量软删除分类
     *
     * @param ids 分类主键集合
     */
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            delete(id);
        }
    }

    /**
     * 更新分类启用状态（0 禁用 / 1 启用）
     *
     * @param id     分类主键
     * @param status 目标状态
     */
    public void updateStatus(Long id, Integer status) {
        Category category = mapper.selectById(id);
        if (category != null) {
            category.setStatus(status);
            mapper.updateById(category);
        }
    }

    /**
     * 更新分类排序值（数值越小越靠前）
     *
     * @param id   分类主键
     * @param sort 排序值
     */
    public void updateSort(Long id, Integer sort) {
        Category category = mapper.selectById(id);
        if (category != null) {
            category.setSort(sort);
            mapper.updateById(category);
        }
    }

    /**
     * 判断分类下是否存在商品
     * <p>查 product 表中 category_id = id 的记录数；&gt; 0 即认为存在商品</p>
     *
     * @param id 分类主键
     * @return 是否存在商品
     */
    public boolean hasProducts(Long id) {
        QueryWrapper<com.example.minimall.model.Product> qw = new QueryWrapper<>();
        qw.eq("category_id", id);
        return productMapper.selectCount(qw) > 0;
    }

    /**
     * 判断分类下是否存在子分类
     *
     * @param id 分类主键
     * @return 存在子分类返回 true
     */
    public boolean hasChildren(Long id) {
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", id).eq("is_deleted", 0);
        return mapper.selectCount(queryWrapper) > 0;
    }
}