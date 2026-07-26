package com.example.minimall.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置视图对象：用于后台配置项的展示与编辑。
 */
@Data
public class SystemConfigVO {
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
