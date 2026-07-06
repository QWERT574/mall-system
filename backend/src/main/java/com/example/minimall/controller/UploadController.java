package com.example.minimall.controller;

import com.example.minimall.annotation.RateLimit;
import com.example.minimall.service.UploadResult;
import com.example.minimall.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片上传控制器
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {
    
    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    
    @Autowired
    private UploadService uploadService;
    
    /**
     * 单图上传
     */
    @RateLimit(limit = 10, timeout = 60)
    @PostMapping("/image")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "subDir", required = false) String subDir) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                response.put("code", 1);
                response.put("message", "上传文件不能为空");
                return response;
            }
            
            UploadResult result = uploadService.uploadImage(file, subDir);
            
            response.put("code", 0);
            response.put("message", "上传成功");
            response.put("data", result);
            
        } catch (Exception e) {
            logger.error("上传图片失败", e);
            response.put("code", 1);
            response.put("message", "上传失败：" + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 多图上传
     */
    @PostMapping("/images")
    public Map<String, Object> uploadImages(@RequestParam("files") MultipartFile[] files,
                                            @RequestParam(value = "subDir", required = false) String subDir) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (files == null || files.length == 0) {
                response.put("code", 1);
                response.put("message", "上传文件不能为空");
                return response;
            }
            
            List<UploadResult> results = uploadService.uploadImages(files, subDir);
            
            // 统计成功和失败数量
            long successCount = results.stream().filter(UploadResult::isSuccess).count();
            long failCount = results.size() - successCount;
            
            response.put("code", 0);
            response.put("message", "上传完成，成功" + successCount + "个，失败" + failCount + "个");
            response.put("data", results);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            
        } catch (Exception e) {
            logger.error("批量上传图片失败", e);
            response.put("code", 1);
            response.put("message", "上传失败：" + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 商品图片上传（专用接口）
     */
    @PostMapping("/product")
    public Map<String, Object> uploadProductImages(@RequestParam("files") MultipartFile[] files) {
        return uploadImages(files, "products");
    }
    
    /**
     * 头像上传（专用接口）
     */
    @PostMapping("/avatar")
    public Map<String, Object> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                response.put("code", 1);
                response.put("message", "上传文件不能为空");
                return response;
            }
            
            // 头像需要压缩成小图
            UploadResult result = uploadService.uploadImage(file, "avatars");
            
            response.put("code", 0);
            response.put("message", "上传成功");
            response.put("data", result);
            
        } catch (Exception e) {
            logger.error("上传头像失败", e);
            response.put("code", 1);
            response.put("message", "上传失败：" + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 活动图片上传（专用接口）
     */
    @PostMapping("/activity")
    public Map<String, Object> uploadActivityImages(@RequestParam("files") MultipartFile[] files) {
        return uploadImages(files, "activities");
    }
    
    /**
     * 评价图片上传（专用接口）
     */
    @PostMapping("/review")
    public Map<String, Object> uploadReviewImages(@RequestParam("files") MultipartFile[] files) {
        return uploadImages(files, "reviews");
    }
    
    /**
     * 删除图片
     */
    @PostMapping("/delete")
    public Map<String, Object> deleteImage(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String url = (String) params.get("url");
            
            if (url == null || url.isEmpty()) {
                response.put("code", 1);
                response.put("message", "图片 URL 不能为空");
                return response;
            }
            
            boolean deleted = uploadService.deleteImage(url);
            
            if (deleted) {
                response.put("code", 0);
                response.put("message", "删除成功");
            } else {
                response.put("code", 1);
                response.put("message", "删除失败，图片不存在");
            }
            
        } catch (Exception e) {
            logger.error("删除图片失败", e);
            response.put("code", 1);
            response.put("message", "删除失败：" + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 批量删除图片
     */
    @PostMapping("/deleteBatch")
    public Map<String, Object> deleteImages(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<String> urls = (List<String>) params.get("urls");
            
            if (urls == null || urls.isEmpty()) {
                response.put("code", 1);
                response.put("message", "图片 URL 列表不能为空");
                return response;
            }
            
            uploadService.deleteImages(urls);
            
            response.put("code", 0);
            response.put("message", "删除成功");
            
        } catch (Exception e) {
            logger.error("批量删除图片失败", e);
            response.put("code", 1);
            response.put("message", "删除失败：" + e.getMessage());
        }
        
        return response;
    }
}
