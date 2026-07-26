package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 活动表 Mapper，对应 activity 表
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    /** 查询指定用户已报名的活动列表 */
    List<Activity> selectJoinedActivities(@Param("userId") Long userId);
}
