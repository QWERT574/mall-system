package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.ProductSpecMapper;
import com.example.minimall.model.ProductSpec;
import org.springframework.stereotype.Service;

import java.util.List;

/** 商品规格服务 */
@Service
public class ProductSpecService {
    /** 商品规格 Mapper */
    private final ProductSpecMapper mapper;

    public ProductSpecService(ProductSpecMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 根据商品 ID 查询全部规格
     *
     * @param productId 商品 ID
     * @return 规格列表
     */
    public List<ProductSpec> findByProductId(Long productId) {
        QueryWrapper<ProductSpec> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        return mapper.selectList(queryWrapper);
    }

    /**
     * 根据规格 ID 查询
     *
     * @param id 规格主键
     * @return 规格实体
     */
    public ProductSpec findById(Long id) {
        return mapper.selectById(id);
    }

    /**
     * 新增或更新规格
     *
     * @param spec 规格实体
     */
    public void save(ProductSpec spec) {
        if (spec.getId() == null) {
            mapper.insert(spec);
        } else {
            mapper.updateById(spec);
        }
    }

    /**
     * 删除单条规格
     *
     * @param id 规格主键
     */
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    /**
     * 删除商品的全部规格（**商品下架/删除时调用**）
     *
     * @param productId 商品 ID
     */
    public void deleteByProductId(Long productId) {
        QueryWrapper<ProductSpec> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        mapper.delete(queryWrapper);
    }
}