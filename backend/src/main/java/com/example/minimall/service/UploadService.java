package com.example.minimall.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** 文件上传服务接口 */
public interface UploadService {
    /** 单图上传 */
    UploadResult uploadImage(MultipartFile file) throws IOException;
    /** 单图上传到指定子目录 */
    UploadResult uploadImage(MultipartFile file, String subDir) throws IOException;
    /** 多图上传 */
    List<UploadResult> uploadImages(MultipartFile[] files) throws IOException;
    /** 多图上传到指定子目录 */
    List<UploadResult> uploadImages(MultipartFile[] files, String subDir) throws IOException;
    /** 根据 URL 删除图片 */
    boolean deleteImage(String url);
    /** 批量删除图片 */
    void deleteImages(List<String> urls);
}
