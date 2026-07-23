package com.example.minimall.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * 静态资源回退控制器 — 文件不存在时返回默认占位图。
 */
@RestController
public class StaticResourceController {

    private static final File UPLOADS_DIR = new File(
        "src/main/resources/static/uploads/products");

    @GetMapping(value = {"/uploads/products/{filename}", "/images/{filename}"})
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        if (filename == null || !filename.matches("^[A-Za-z0-9._-]{1,128}$")) {
            return ResponseEntity.notFound().build();
        }
        try {
            File file = new File(UPLOADS_DIR, filename);
            if (file.exists() && file.isFile()) {
                return ResponseEntity.ok()
                    .contentType(detectType(filename))
                    .body(new FileSystemResource(file));
            }
        } catch (Exception ignored) {}
        // 任意文件缺失时返回默认 SVG 占位图
        Resource fallback = new ClassPathResource("static/images/product-default.svg");
        if (fallback.exists()) {
            return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(fallback);
        }
        return ResponseEntity.notFound().build();
    }

    private MediaType detectType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
