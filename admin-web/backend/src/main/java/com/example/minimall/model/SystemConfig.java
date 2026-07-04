package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置实体，对应 system_config 表，存储键值对形式的系统配置
 */
@Data
@TableName("system_config")
public class SystemConfig {
    @TableId
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}