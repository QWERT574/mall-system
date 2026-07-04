package com.example.minimall.service.impl;

import com.example.minimall.config.UploadConfig;
import com.example.minimall.service.UploadResult;
import com.example.minimall.service.UploadService;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 图片上传服务
 */
@Service
public class UploadServiceImpl implements UploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(UploadServiceImpl.class);

    /** 上传相关配置 */
    @Autowired
    private UploadConfig uploadConfig;
    
    /**
     * 单图上传（**委托给带子目录版本**）
     *
     * @param file 上传文件
     * @return 上传结果
     */
    public UploadResult uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, null);
    }

    /**
     * 单图上传（**完整流程**）
     * <p>
     * 流程：文件校验 → 生成 UUID 文件名 → 创建目录 → 落盘 → 压缩 → （可选）水印 → 生成访问 URL
     * </p>
     *
     * @param file   上传文件
     * @param subDir 自定义子目录（可空）
     * @return 上传结果（含 url、fileName、fileSize 等）
     */
    public UploadResult uploadImage(MultipartFile file, String subDir) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = generateFileName() + extension;
        
        // 创建上传目录
        String uploadPath = getUploadPath(subDir);
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        // 保存文件
        Path filePath = uploadDir.resolve(fileName);
        File destFile = filePath.toFile();
        file.transferTo(destFile);
        
        // 压缩图片
        File compressedFile = compressImage(destFile, extension);
        
        // 添加水印
        if (uploadConfig.getWatermarkEnabled()) {
            addWatermark(compressedFile, extension);
        }
        
        // 生成访问 URL
        String url = generateUrl(compressedFile.getName(), subDir);
        
        // 返回结果
        UploadResult result = new UploadResult();
        result.setSuccess(true);
        result.setFileName(compressedFile.getName());
        result.setOriginalName(originalFilename);
        result.setFileSize(compressedFile.length());
        result.setUrl(url);
        result.setPath(compressedFile.getAbsolutePath());
        
        return result;
    }
    
    /**
     * 多图上传
     */
    public List<UploadResult> uploadImages(MultipartFile[] files) throws IOException {
        return uploadImages(files, null);
    }
    
    /**
     * 多图上传（指定子目录）
     */
    public List<UploadResult> uploadImages(MultipartFile[] files, String subDir) throws IOException {
        List<UploadResult> results = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    UploadResult result = uploadImage(file, subDir);
                    results.add(result);
                } catch (Exception e) {
                    logger.error("上传图片失败：{}", file.getOriginalFilename(), e);
                    UploadResult errorResult = new UploadResult();
                    errorResult.setSuccess(false);
                    errorResult.setMessage("上传失败：" + e.getMessage());
                    errorResult.setOriginalName(file.getOriginalFilename());
                    results.add(errorResult);
                }
            }
        }
        
        return results;
    }
    
    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) throws IOException {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new IOException("上传文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > uploadConfig.getMaxSize()) {
            throw new IOException("文件大小不能超过 " + (uploadConfig.getMaxSize() / 1024 / 1024) + "MB");
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedType(contentType)) {
            throw new IOException("不支持的图片格式，仅支持：" + String.join(", ", uploadConfig.getAllowedTypes()));
        }
    }
    
    /**
     * 检查文件类型是否允许
     */
    private boolean isAllowedType(String contentType) {
        return Arrays.stream(uploadConfig.getAllowedTypes())
                .anyMatch(type -> type.equalsIgnoreCase(contentType));
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 获取上传路径
     */
    private String getUploadPath(String subDir) {
        StringBuilder path = new StringBuilder(uploadConfig.getLocation());
        
        // 添加日期子目录
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String datePath = dateFormat.format(new Date());
        path.append("/").append(datePath);
        
        // 添加自定义子目录
        if (subDir != null && !subDir.isEmpty()) {
            path.append("/").append(subDir);
        }
        
        return path.toString();
    }
    
    /**
     * 压缩图片
     */
    private File compressImage(File sourceFile, String extension) throws IOException {
        // 如果是 GIF 图片，不压缩
        if (".gif".equalsIgnoreCase(extension)) {
            return sourceFile;
        }
        
        String outputFileName = sourceFile.getName();
        String outputFilePath = sourceFile.getAbsolutePath();
        
        // 如果文件名不包含 UUID，生成新文件名
        if (!outputFileName.contains(UUID.randomUUID().toString().substring(0, 8))) {
            String fileNameWithoutExt = outputFileName.substring(0, outputFileName.lastIndexOf("."));
            outputFileName = fileNameWithoutExt + "_compressed" + extension;
            outputFilePath = sourceFile.getParent() + File.separator + outputFileName;
        }
        
        File outputFile = new File(outputFilePath);
        
        try {
            // 使用 Thumbnailator 压缩图片
            Thumbnails.of(sourceFile)
                    .size(uploadConfig.getMaxWidth(), uploadConfig.getMaxHeight())
                    .outputQuality(uploadConfig.getCompressQuality() / 100.0)
                    .outputFormat("JPEG")
                    .toFile(outputFile);
            
            // 删除原文件
            if (sourceFile.exists() && !sourceFile.equals(outputFile)) {
                sourceFile.delete();
            }
            
            logger.info("图片压缩成功：{} -> {}", sourceFile.length(), outputFile.length());
        } catch (Exception e) {
            logger.error("图片压缩失败，使用原文件", e);
            // 如果压缩失败，返回原文件
            return sourceFile;
        }
        
        return outputFile;
    }
    
    /**
     * 添加水印
     */
    private void addWatermark(File imageFile, String extension) {
        try {
            // 跳过 GIF 图片
            if (".gif".equalsIgnoreCase(extension)) {
                return;
            }
            
            String outputFileName = imageFile.getName();
            String outputFilePath = imageFile.getAbsolutePath();
            
            // 生成带水印的文件名
            String fileNameWithoutExt = outputFileName.substring(0, outputFileName.lastIndexOf("."));
            outputFileName = fileNameWithoutExt + "_watermark" + extension;
            outputFilePath = imageFile.getParent() + File.separator + outputFileName;
            
            File outputFile = new File(outputFilePath);
            
            // 根据位置设置水印
            Positions position = Positions.BOTTOM_RIGHT;
            switch (uploadConfig.getWatermarkPosition()) {
                case "center":
                    position = Positions.CENTER;
                    break;
                case "top-left":
                    position = Positions.TOP_LEFT;
                    break;
                case "top-right":
                    position = Positions.TOP_RIGHT;
                    break;
                case "bottom-left":
                    position = Positions.BOTTOM_LEFT;
                    break;
                default:
                    position = Positions.BOTTOM_RIGHT;
            }
            
            // 创建水印图片
            BufferedImage watermarkImage = new BufferedImage(
                uploadConfig.getWatermarkText().length() * 12, 30, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = watermarkImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Font font = new Font("Microsoft YaHei", Font.BOLD, 24);
            g2d.setFont(font);
            g2d.setColor(new Color(255, 255, 255, (int)(uploadConfig.getWatermarkOpacity() / 100.0f * 255)));
            g2d.drawString(uploadConfig.getWatermarkText(), 0, 24);
            g2d.dispose();
            
            // 添加水印
            Thumbnails.of(imageFile)
                    .watermark(position, watermarkImage, 0.5f)
                    .outputQuality(0.9)
                    .toFile(outputFile);
            
            // 删除原文件
            if (imageFile.exists() && !imageFile.equals(outputFile)) {
                imageFile.delete();
            }
            
            logger.info("水印添加成功：{}", outputFile.getName());
        } catch (Exception e) {
            logger.error("水印添加失败", e);
        }
    }
    
    /**
     * 生成访问 URL
     */
    private String generateUrl(String fileName, String subDir) {
        StringBuilder url = new StringBuilder(uploadConfig.getUrlPrefix());
        
        // 添加日期路径
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        url.append(dateFormat.format(new Date())).append("/");
        
        // 添加子目录
        if (subDir != null && !subDir.isEmpty()) {
            url.append(subDir).append("/");
        }
        
        url.append(fileName);
        
        return url.toString();
    }
    
    /**
     * 删除图片
     * <p>根据 URL 反推本地文件路径后删除</p>
     *
     * @param url 图片访问 URL
     * @return 是否成功删除
     */
    public boolean deleteImage(String url) {
        try {
            // 从 URL 提取文件路径
            String filePath = url.replace(uploadConfig.getUrlPrefix(), uploadConfig.getLocation() + "/");
            filePath = filePath.replace("/", File.separator);
            
            File file = new File(filePath);
            if (file.exists()) {
                return file.delete();
            }
        } catch (Exception e) {
            logger.error("删除图片失败：{}", url, e);
        }
        return false;
    }
    
    /**
     * 批量删除图片
     */
    public void deleteImages(List<String> urls) {
        for (String url : urls) {
            deleteImage(url);
        }
    }
}
