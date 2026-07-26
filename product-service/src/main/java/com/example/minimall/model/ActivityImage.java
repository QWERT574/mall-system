package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动图片实体，对应 activity_image 表，存储活动关联的图片资源
 */
@Data
@TableName("activity_image")
public class ActivityImage {
    @TableId
    private Long id;
    private Long activityId;
    private String imageUrl;
    private Integer sort;
    private LocalDateTime createdAt;
}