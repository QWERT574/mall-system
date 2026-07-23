package com.example.minimall.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 静态资源控制器 — 替代 backend 提供图片等静态文件。
 * 图片存放在 product-service/src/main/resources/static/ 下。
 */
@RestController
public class StaticResourceController {

    @GetMapping(value = {"/uploads/products/{filename}", "/images/{filename}"})
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        // 安全校验：只允许安全字符
        if (filename == null || !filename.matches("^[A-Za-z0-9._-]{1,128}$")) {
            return ResponseEntity.notFound().build();
        }
        try {
            Resource resource = new FileSystemResource(
                "src/main/resources/static/uploads/products/" + filename);
            if (resource.exists()) {
                return ResponseEntity.ok()
                    .contentType(detectMediaType(filename))
                    .body(resource);
            }
        } catch (Exception ignored) {}
        return ResponseEntity.notFound().build();
    }

    private MediaType detectMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
