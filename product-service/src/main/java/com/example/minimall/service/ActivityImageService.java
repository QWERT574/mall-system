package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.ActivityImageMapper;
import com.example.minimall.model.ActivityImage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 活动图片管理服务 */
@Service
public class ActivityImageService {

    /** 活动图片 Mapper */
    private final ActivityImageMapper mapper;

    public ActivityImageService(ActivityImageMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 根据活动 ID 查询活动图片列表
     *
     * @param activityId 活动 ID
     * @return 图片列表
     * @throws IllegalArgumentException 当 activityId 为空时
     */
    public List<ActivityImage> findByActivityId(Long activityId) {
        if (activityId == null) {
            throw new IllegalArgumentException("活动ID不能为空");
        }
        return mapper.selectByActivityId(activityId);
    }

    /**
     * 新增或更新活动图片（**根据 ID 是否为空判断**）
     *
     * @param image 图片实体
     * @return 保存后的实体
     */
    @Transactional(rollbackFor = Exception.class)
    public ActivityImage save(ActivityImage image) {
        if (image.getActivityId() == null) {
            throw new IllegalArgumentException("活动ID不能为空");
        }
        if (image.getImageUrl() == null || image.getImageUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("图片URL不能为空");
        }
        if (image.getId() == null) {
            mapper.insert(image);
        } else {
            mapper.updateById(image);
        }
        return image;
    }

    /**
     * 根据图片 ID 删除单张活动图片
     *
     * @param id 图片主键
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("图片ID不能为空");
        }
        mapper.deleteById(id);
    }

    /**
     * 删除活动的所有图片（**事务**）
     * <p>活动被删除时通常会级联调用</p>
     *
     * @param activityId 活动 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByActivityId(Long activityId) {
        if (activityId == null) {
            throw new IllegalArgumentException("活动ID不能为空");
        }
        QueryWrapper<ActivityImage> qw = new QueryWrapper<>();
        qw.eq("activity_id", activityId);
        mapper.delete(qw);
    }

    /**
     * 设置活动封面图（**约定 sort=0 的图片为封面**）
     * <p>设置规则：把传入的 imageId 的 sort 置为 0；若存在其它 sort=0 图片则置为 1</p>
     *
     * @param activityId 活动 ID
     * @param imageId    要设为封面的图片 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void setCover(Long activityId, Long imageId) {
        if (activityId == null || imageId == null) {
            throw new IllegalArgumentException("活动ID和图片ID不能为空");
        }
        QueryWrapper<ActivityImage> qw = new QueryWrapper<>();
        qw.eq("activity_id", activityId);
        List<ActivityImage> images = mapper.selectList(qw);
        for (ActivityImage img : images) {
            if (img.getId().equals(imageId)) {
                img.setSort(0);
            } else if (img.getSort() != null && img.getSort() == 0) {
                img.setSort(1);
            }
            mapper.updateById(img);
        }
    }
}
