package com.example.minimall.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 商品图片资源读取接口（用于替代外链，统一从本地类路径提供图片）
 */
@RestController
@RequestMapping("/api/image")
public class ImageController {

    /**
     * 按文件名获取商品图片，缺失时回退到默认占位图
     */
    @GetMapping("/products/{filename}")
    public ResponseEntity<byte[]> getProductImage(@PathVariable String filename) {
        try {
            // 从uploads/products目录加载图片
            Resource resource = new ClassPathResource("static/uploads/products/" + filename);
            
            if (!resource.exists()) {
                // 如果文件不存在，返回默认图片
                resource = new ClassPathResource("static/images/product-default.svg");
            }
            
            byte[] imageBytes = readInputStream(resource.getInputStream());
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setCacheControl("max-age=3600");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageBytes);
                    
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    private byte[] readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
