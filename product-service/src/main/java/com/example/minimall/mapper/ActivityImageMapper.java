package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ActivityImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 活动图片表 Mapper，对应 activity_image 表
 */
@Mapper
public interface ActivityImageMapper extends BaseMapper<ActivityImage> {

    /** 根据活动 ID 查询图片列表（按 sort 升序） */
    @Select("SELECT * FROM activity_image WHERE activity_id = #{activityId} ORDER BY sort ASC")
    List<ActivityImage> selectByActivityId(Long activityId);

    /** 统计指定活动的图片数量 */
    @Select("SELECT COUNT(*) FROM activity_image WHERE activity_id = #{activityId}")
    int countByActivityId(Long activityId);
}
