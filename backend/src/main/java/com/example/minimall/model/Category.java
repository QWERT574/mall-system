package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类实体，对应 category 表，存储商品分类树形结构
 */
@Data
@TableName("category")
public class Category {
    @TableId
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private String icon;
    private String description;
    private Integer sort;
    private Integer status;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}