package com.example.minimall.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.ActivityMapper;
import com.example.minimall.mapper.ActivityParticipantMapper;
import com.example.minimall.model.Activity;
import com.example.minimall.model.ActivityParticipant;
import com.example.minimall.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 活动与活动报名服务 */
@Service
public class ActivityService {
    /** 活动 Mapper */
    private final ActivityMapper activityMapper;
    /** 活动报名 Mapper */
    private final ActivityParticipantMapper participantMapper;

    public ActivityService(ActivityMapper activityMapper, ActivityParticipantMapper participantMapper) {
        this.activityMapper = activityMapper;
        this.participantMapper = participantMapper;
    }

    /** 查询所有活动（**包含已下线**） */
    public List<Activity> findAll() {
        return activityMapper.selectList(null);
    }

    /**
     * 根据 ID 查询活动
     *
     * @param id 活动主键
     * @return 活动实体，未找到返回 null
     */
    public Activity findById(Long id) {
        return activityMapper.selectById(id);
    }

    /**
     * 新增或更新活动（**根据 ID 是否为空判断**）
     * <p>新增时自动写入 created_at / updated_at / created_by；更新时只刷新 updated_at</p>
     *
     * @param activity 活动实体
     */
    public void save(Activity activity) {
        if (activity.getId() == null) {
            activity.setCreatedAt(LocalDateTime.now());
            activity.setUpdatedAt(LocalDateTime.now());
            // 从 Spring Security 上下文取当前登录用户 ID；未登录时兜底为 1
            Long currentUserId = SecurityUtil.getCurrentUserId();
            activity.setCreatedBy(currentUserId != null ? currentUserId : 1L);
            activityMapper.insert(activity);
        } else {
            activity.setUpdatedAt(LocalDateTime.now());
            activityMapper.updateById(activity);
        }
    }

    /**
     * 删除活动（**真实删除**，无软删标志）
     *
     * @param id 活动主键
     */
    public void delete(Long id) {
        activityMapper.deleteById(id);
    }

    /**
     * 根据状态查询活动列表
     * <p>状态值由 {@code ActivityStatusEnum} 决定</p>
     *
     * @param status 状态值
     * @return 活动列表
     */
    public List<Activity> findByStatus(Integer status) {
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        return activityMapper.selectList(queryWrapper);
    }

    /**
     * 根据活动类型查询
     *
     * @param type 活动类型（具体值参考枚举）
     * @return 活动列表
     */
    public List<Activity> findByType(Integer type) {
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_type", type);
        return activityMapper.selectList(queryWrapper);

    }

    /**
     * 用户报名参加活动
     * <p>
     * 校验流程：
     * <ol>
     *   <li>活动是否存在</li>
     *   <li>活动状态合法（0 未开始 / 1 进行中）</li>
     *   <li>报名时间未结束</li>
     *   <li>未超过最大参与人数</li>
     *   <li>用户未重复报名</li>
     * </ol>
     * 全部通过后插入报名记录并把活动当前人数 +1。
     * </p>
     *
     * @param activityId      活动 ID
     * @param userId          用户 ID
     * @param participantName 参与者姓名
     * @param participantPhone 参与者手机号
     */
    @Transactional(rollbackFor = Exception.class)
    public void joinActivity(Long activityId, Long userId, String participantName, String participantPhone) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }

        // 检查活动状态
        if (activity.getStatus() != 0 && activity.getStatus() != 1) {
            throw new IllegalArgumentException("活动已结束或已取消，无法报名");
        }

        // 检查活动时间
        if (LocalDateTime.now().isAfter(activity.getEndTime())) {
            throw new IllegalArgumentException("活动报名已结束");
        }

        // 检查是否已达到最大参与人数
        if (activity.getMaxParticipants() > 0 && activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            throw new IllegalArgumentException("活动已达到最大参与人数");
        }

        // 检查是否已报名
        QueryWrapper<ActivityParticipant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId).eq("user_id", userId);
        ActivityParticipant existingParticipant = participantMapper.selectOne(queryWrapper);
        if (existingParticipant != null) {
            throw new IllegalArgumentException("您已报名该活动");
        }

        // 创建报名记录
        ActivityParticipant participant = new ActivityParticipant();
        participant.setActivityId(activityId);
        participant.setUserId(userId);
        participant.setParticipantName(participantName);
        participant.setParticipantPhone(participantPhone);
        participant.setStatus(0); // 待审核
        participant.setCreatedAt(LocalDateTime.now());
        participant.setUpdatedAt(LocalDateTime.now());
        participantMapper.insert(participant);

        // 更新活动当前参与人数
        activity.setCurrentParticipants(activity.getCurrentParticipants() + 1);
        activityMapper.updateById(activity);
    }

    // 取消报名
    @Transactional(rollbackFor = Exception.class)
    public void cancelJoin(Long activityId, Long userId) {
        // 删除报名记录
        QueryWrapper<ActivityParticipant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId).eq("user_id", userId);
        int result = participantMapper.delete(queryWrapper);
        if (result > 0) {
            // 更新活动当前参与人数
            Activity activity = activityMapper.selectById(activityId);
            if (activity != null) {
                activity.setCurrentParticipants(activity.getCurrentParticipants() - 1);
                activityMapper.updateById(activity);
            }
        }
    }

    /** 查询活动所有参与者记录 */
    public List<ActivityParticipant> findParticipantsByActivityId(Long activityId) {
        QueryWrapper<ActivityParticipant> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId);
        return participantMapper.selectList(queryWrapper);
    }

    /**
     * 查询用户已报名的活动列表
     * <p>通过自定义 SQL 联表查询</p>
     *
     * @param userId 用户 ID
     * @return 活动列表
     */
    public List<Activity> findJoinedActivities(Long userId) {
        return activityMapper.selectJoinedActivities(userId);
    }

    /**
     * 更新活动状态
     *
     * @param id     活动 ID
     * @param status 目标状态
     */
    public void updateStatus(Long id, Integer status) {
        Activity activity = activityMapper.selectById(id);
        if (activity != null) {
            activity.setStatus(status);
            activity.setUpdatedAt(LocalDateTime.now());
            activityMapper.updateById(activity);
        }
    }

    /**
     * 更新参与者审核状态（0 待审核 / 1 通过 / 2 拒绝）
     *
     * @param participantId 参与者记录 ID
     * @param status        审核状态
     */
    public void updateParticipantStatus(Long participantId, Integer status) {
        ActivityParticipant participant = participantMapper.selectById(participantId);
        if (participant != null) {
            participant.setStatus(status);
            participant.setUpdatedAt(LocalDateTime.now());
            participantMapper.updateById(participant);
        }
    }

    /** 查询推荐活动列表（按 recommend_order 升序、再按创建时间倒序） */
    public List<Activity> findRecommendedActivities() {
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_recommended", 1)
                    .orderByAsc("recommend_order")
                    .orderByDesc("created_at");
        return activityMapper.selectList(queryWrapper);
    }

    /**
     * 根据关键词搜索活动（**匹配名称/描述/地点**）
     *
     * @param keyword 关键词
     * @return 活动列表
     */
    public List<Activity> findByKeyword(String keyword) {
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper ->
            wrapper.like("name", keyword)
                   .or()
                   .like("description", keyword)
                   .or()
                   .like("location", keyword)
        );
        queryWrapper.orderByDesc("created_at");
        return activityMapper.selectList(queryWrapper);
    }
}
