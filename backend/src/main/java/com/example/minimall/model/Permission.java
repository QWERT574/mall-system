package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体，对应 permission 表，存储系统权限点及层级关系
 */
@Data
@TableName("permission")
public class Permission {
    @TableId
    private Long id;
    private String name;
    private String code;
    private String description;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}