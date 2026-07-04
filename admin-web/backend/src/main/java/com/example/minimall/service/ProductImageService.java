package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.ProductImageMapper;
import com.example.minimall.model.ProductImage;
import org.springframework.stereotype.Service;

import java.util.List;

/** 商品图片管理服务 */
@Service
public class ProductImageService {
    /** 商品图片 Mapper */
    private final ProductImageMapper mapper;

    public ProductImageService(ProductImageMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 根据商品 ID 查询图片列表（**按 sort 升序**）
     *
     * @param productId 商品 ID
     * @return 商品图片列表
     */
    public List<ProductImage> findByProductId(Long productId) {
        QueryWrapper<ProductImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId).orderByAsc("sort");
        return mapper.selectList(queryWrapper);
    }

    /**
     * 新增或更新商品图片（**根据 ID 是否为空判断**）
     *
     * @param image 图片实体
     */
    public void save(ProductImage image) {
        if (image.getId() == null) {
            mapper.insert(image);
        } else {
            mapper.updateById(image);
        }
    }

    /**
     * 删除单张商品图片
     *
     * @param id 图片主键
     */
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    /**
     * 删除商品的所有图片（**商品下架/删除时调用**）
     *
     * @param productId 商品 ID
     */
    public void deleteByProductId(Long productId) {
        QueryWrapper<ProductImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        mapper.delete(queryWrapper);
    }

    /**
     * 设置商品封面图
     * <p>实现逻辑：先把该商品下所有图片 isCover 置 0，再把指定图片 isCover 置 1</p>
     *
     * @param productId 商品 ID
     * @param imageId   要设为封面的图片 ID
     */
    public void setCover(Long productId, Long imageId) {
        // 先将所有图片设置为非封面
        QueryWrapper<ProductImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        ProductImage updateImage = new ProductImage();
        updateImage.setIsCover(0);
        mapper.update(updateImage, queryWrapper);
        
        // 再将指定图片设置为封面
        ProductImage coverImage = new ProductImage();
        coverImage.setId(imageId);
        coverImage.setIsCover(1);
        mapper.updateById(coverImage);
    }
}