package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ActivityParticipant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 活动参与记录 Mapper，对应 activity_participant 表
 */
@Mapper
public interface ActivityParticipantMapper extends BaseMapper<ActivityParticipant> {
}
