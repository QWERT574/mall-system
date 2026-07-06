package com.example.minimall.vo;

import java.util.List;

/**
 * 通用分页结果包装。
 *
 * <p>字段：total 总记录数、records 当前页数据、current 当前页码、size 每页大小、pages 总页数。
 */
public class PageInfo<T> {
    private Long total;
    private List<T> records;
    private Long current;
    private Long size;
    private Long pages;
    
    public Long getTotal() {
        return total;
    }
    
    public void setTotal(Long total) {
        this.total = total;
    }
    
    public List<T> getRecords() {
        return records;
    }
    
    public void setRecords(List<T> records) {
        this.records = records;
    }
    
    public Long getCurrent() {
        return current;
    }
    
    public void setCurrent(Long current) {
        this.current = current;
    }
    
    public Long getSize() {
        return size;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public Long getPages() {
        return pages;
    }
    
    public void setPages(Long pages) {
        this.pages = pages;
    }
}
