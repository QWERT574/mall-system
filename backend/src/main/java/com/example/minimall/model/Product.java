package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 商品实体，对应 product 表，存储商品基础信息、库存与价格
 */
@TableName("product")
public class Product {
    @TableId
    private Long id;
    private String name;
    private String cover;
    private BigDecimal price;
    private Integer stock;
    private String description;
    private Long categoryId;
    private Long sellerId;
    // 销量（虚拟字段，从订单中计算）
    @TableField(exist = false)
    private Integer sales = 0;
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCover() {
        return cover;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public Integer getStock() {
        return stock;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public Integer getSales() {
        return sales;
    }
    
    public Long getSellerId() {
        return sellerId;
    }
    
    // Setters
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setCover(String cover) {
        this.cover = cover;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public void setStock(Integer stock) {
        this.stock = stock;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }
    
    public void setSales(Integer sales) {
        this.sales = sales;
    }
}
