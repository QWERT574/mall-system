package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品评价实体，对应 product_review 表，存储用户对商品的评价内容与评分
 */
@Data
@TableName("product_review")
public class ProductReview {
    @TableId
    private Long id;
    private Long productId;
    private Long orderId;
    private Long userId;
    private Integer rating;
    private String content;
    private String images;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 非数据库字段，用于存储回复
    @TableField(exist = false)
    private ReviewReply reply;
    
    // 非数据库字段，用于存储回复列表
    @TableField(exist = false)
    private java.util.List<ReviewReply> replies;
    
    // 非数据库字段，用于存储用户名
    @TableField(exist = false)
    private String userName;
    
    public ReviewReply getReply() {
        return reply;
    }
    
    public void setReply(ReviewReply reply) {
        this.reply = reply;
    }
    
    public java.util.List<ReviewReply> getReplies() {
        return replies;
    }
    
    public void setReplies(java.util.List<ReviewReply> replies) {
        this.replies = replies;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}