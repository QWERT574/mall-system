package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.ProductTagMapper;
import com.example.minimall.mapper.ProductTagRelationMapper;
import com.example.minimall.model.ProductTag;
import com.example.minimall.model.ProductTagRelation;
import org.springframework.stereotype.Service;

import java.util.List;

/** 商品标签与标签关联服务 */
@Service
public class ProductTagService {
    /** 标签 Mapper */
    private final ProductTagMapper tagMapper;
    /** 标签关联 Mapper */
    private final ProductTagRelationMapper relationMapper;

    public ProductTagService(ProductTagMapper tagMapper, ProductTagRelationMapper relationMapper) {
        this.tagMapper = tagMapper;
        this.relationMapper = relationMapper;
    }

    /** 查询所有标签 */
    public List<ProductTag> findAll() {
        return tagMapper.selectList(null);
    }

    /**
     * 根据标签 ID 查询
     *
     * @param id 标签主键
     * @return 标签实体
     */
    public ProductTag findById(Long id) {
        return tagMapper.selectById(id);
    }

    /**
     * 新增或更新标签
     *
     * @param tag 标签实体
     */
    public void save(ProductTag tag) {
        if (tag.getId() == null) {
            tagMapper.insert(tag);
        } else {
            tagMapper.updateById(tag);
        }
    }

    /**
     * 删除标签（**真实删除**，不级联清理关联）
     *
     * @param id 标签主键
     */
    public void delete(Long id) {
        tagMapper.deleteById(id);
    }

    /**
     * 查询某商品的全部标签
     *
     * @param productId 商品 ID
     * @return 标签列表
     */
    public List<ProductTag> findByProductId(Long productId) {
        return tagMapper.selectByProductId(productId);
    }

    /**
     * 为商品添加一个标签（**插入关联表**）
     *
     * @param productId 商品 ID
     * @param tagId     标签 ID
     */
    public void addTagToProduct(Long productId, Long tagId) {
        ProductTagRelation relation = new ProductTagRelation();
        relation.setProductId(productId);
        relation.setTagId(tagId);
        relationMapper.insert(relation);
    }

    /**
     * 移除商品的某个标签（**删除关联表**）
     *
     * @param productId 商品 ID
     * @param tagId     标签 ID
     */
    public void removeTagFromProduct(Long productId, Long tagId) {
        QueryWrapper<ProductTagRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId).eq("tag_id", tagId);
        relationMapper.delete(queryWrapper);
    }

    /**
     * 重新设置商品的标签列表（**先清空再添加**，完全覆盖）
     *
     * @param productId 商品 ID
     * @param tagIds    新的标签 ID 列表
     */
    public void setTagsForProduct(Long productId, List<Long> tagIds) {
        // 清除现有标签
        QueryWrapper<ProductTagRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        relationMapper.delete(queryWrapper);

        // 添加新标签
        for (Long tagId : tagIds) {
            addTagToProduct(productId, tagId);
        }
    }

    /**
     * 清除商品的**全部**标签关联
     *
     * @param productId 商品 ID
     */
    public void deleteByProductId(Long productId) {
        QueryWrapper<ProductTagRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        relationMapper.delete(queryWrapper);
    }
}
