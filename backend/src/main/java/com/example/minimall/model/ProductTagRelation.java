package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 商品与标签关联实体，对应 product_tag_relation 表，存储商品与标签的多对多关系
 */
@TableName("product_tag_relation")
public class ProductTagRelation {
    @TableId(type = IdType.INPUT)
    private Long productId;
    private Long tagId;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}