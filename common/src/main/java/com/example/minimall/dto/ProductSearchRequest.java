package com.example.minimall.dto;

/**
 * 商品搜索请求参数。
 *
 * <p>支持按关键字、分类、价格区间、排序字段、当前用户以及分页参数进行组合查询。
 */
@SuppressWarnings("unused")
public class ProductSearchRequest {
    private String keyword;
    
    private Long categoryId;
    
    private Double minPrice;
    
    private Double maxPrice;
    
    private String sortBy;
    
    private String sortOrder;
    
    private Long userId;
    
    private Integer page = 1;
    
    private Integer pageSize = 20;
}
