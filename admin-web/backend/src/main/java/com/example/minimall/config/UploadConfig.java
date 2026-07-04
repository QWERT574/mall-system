package com.example.minimall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 图片上传配置
 */
@Configuration
@ConfigurationProperties(prefix = "upload")
public class UploadConfig {
    
    /**
     * 上传路径
     */
    private String location = "uploads";
    
    /**
     * 最大文件大小（字节）默认 10MB
     */
    private Long maxSize = 10 * 1024 * 1024L;
    
    /**
     * 允许的图片格式
     */
    private String[] allowedTypes = {"image/jpeg", "image/png", "image/gif", "image/webp"};
    
    /**
     * 图片压缩质量（0-100）
     */
    private Integer compressQuality = 80;
    
    /**
     * 图片最大宽度
     */
    private Integer maxWidth = 1920;
    
    /**
     * 图片最大高度
     */
    private Integer maxHeight = 1920;
    
    /**
     * 是否添加水印
     */
    private Boolean watermarkEnabled = false;
    
    /**
     * 水印文字
     */
    private String watermarkText = "乡村振兴农产品平台";
    
    /**
     * 水印位置（center, top-left, top-right, bottom-left, bottom-right）
     */
    private String watermarkPosition = "bottom-right";
    
    /**
     * 水印透明度（0-100）
     */
    private Integer watermarkOpacity = 50;
    
    /**
     * 访问 URL 前缀
     */
    private String urlPrefix = "/uploads/";

    // Getters and Setters
    /**
     * 获取文件上传根目录。
     */
    public String getLocation() {
        return location;
    }

    /**
     * 设置文件上传根目录。
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 获取单次上传最大字节数。
     */
    public Long getMaxSize() {
        return maxSize;
    }

    /**
     * 设置单次上传最大字节数。
     */
    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * 获取允许的图片 MIME 类型数组。
     */
    public String[] getAllowedTypes() {
        return allowedTypes;
    }

    /**
     * 设置允许的图片 MIME 类型数组。
     */
    public void setAllowedTypes(String[] allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    /**
     * 获取图片压缩质量（0-100）。
     */
    public Integer getCompressQuality() {
        return compressQuality;
    }

    /**
     * 设置图片压缩质量（0-100）。
     */
    public void setCompressQuality(Integer compressQuality) {
        this.compressQuality = compressQuality;
    }

    /**
     * 获取图片最大宽度（像素）。
     */
    public Integer getMaxWidth() {
        return maxWidth;
    }

    /**
     * 设置图片最大宽度（像素）。
     */
    public void setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
    }

    /**
     * 获取图片最大高度（像素）。
     */
    public Integer getMaxHeight() {
        return maxHeight;
    }

    /**
     * 设置图片最大高度（像素）。
     */
    public void setMaxHeight(Integer maxHeight) {
        this.maxHeight = maxHeight;
    }

    /**
     * 是否启用水印。
     */
    public Boolean getWatermarkEnabled() {
        return watermarkEnabled;
    }

    /**
     * 设置是否启用水印。
     */
    public void setWatermarkEnabled(Boolean watermarkEnabled) {
        this.watermarkEnabled = watermarkEnabled;
    }

    /**
     * 获取水印文字内容。
     */
    public String getWatermarkText() {
        return watermarkText;
    }

    /**
     * 设置水印文字内容。
     */
    public void setWatermarkText(String watermarkText) {
        this.watermarkText = watermarkText;
    }

    /**
     * 获取水印位置（如 center、top-left 等）。
     */
    public String getWatermarkPosition() {
        return watermarkPosition;
    }

    /**
     * 设置水印位置。
     */
    public void setWatermarkPosition(String watermarkPosition) {
        this.watermarkPosition = watermarkPosition;
    }

    /**
     * 获取水印透明度（0-100）。
     */
    public Integer getWatermarkOpacity() {
        return watermarkOpacity;
    }

    /**
     * 设置水印透明度（0-100）。
     */
    public void setWatermarkOpacity(Integer watermarkOpacity) {
        this.watermarkOpacity = watermarkOpacity;
    }

    /**
     * 获取文件访问 URL 前缀。
     */
    public String getUrlPrefix() {
        return urlPrefix;
    }

    /**
     * 设置文件访问 URL 前缀。
     */
    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }
}
