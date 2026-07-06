package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.common.Result;
import com.example.minimall.model.Activity;
import com.example.minimall.model.ActivityParticipant;
import com.example.minimall.service.ActivityService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 营销活动（报名/抽奖等）相关接口控制器 */
@RestController
@RequestMapping("/api/activity")
public class ActivityController extends BaseController {
    /** 活动业务服务 */
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /** 分页查询活动列表，支持按关键字/状态/类型筛选 */
    @GetMapping("/list")
    public Result<Map<String, Object>> getActivityList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword) {
        try {
            // 计算偏移量
            int offset = (page - 1) * size;

            // 查询活动列表
            List<Activity> activities;
            if (keyword != null && !keyword.isEmpty()) {
                activities = activityService.findByKeyword(keyword);
            } else if (status != null) {
                activities = activityService.findByStatus(status);
            } else if (type != null) {
                activities = activityService.findByType(type);
            } else {
                activities = activityService.findAll();
            }

            // 分页处理
            int total = activities.size();
            int end = Math.min(offset + size, total);
            List<Activity> paginatedActivities = activities.subList(offset, end);

            // 构建分页响应
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("records", paginatedActivities);
            pageData.put("total", total);
            pageData.put("current", page);
            pageData.put("size", size);
            pageData.put("pages", (int) Math.ceil((double) total / size));

            return success(pageData);
        } catch (Exception e) {
            e.printStackTrace();
            return error("获取活动列表失败: " + e.getMessage());
        }
    }

    /** 根据 ID 获取活动详情 */
    @GetMapping("/{id}")
    public Result<Activity> getActivityDetail(@PathVariable Long id) {
        try {
            Activity activity = activityService.findById(id);
            return success(activity);
        } catch (Exception e) {
            e.printStackTrace();
            return error("获取活动详情失败: " + e.getMessage());
        }
    }

    /** 根据状态获取活动列表 */
    @GetMapping("/list/status/{status}")
    public Result<List<Activity>> getActivityListByStatus(@PathVariable Integer status) {
        try {
            List<Activity> activities = activityService.findByStatus(status);
            return success(activities);
        } catch (Exception e) {
            e.printStackTrace();
            return error("获取活动列表失败: " + e.getMessage());
        }
    }

    /** 根据类型获取活动列表 */
    @GetMapping("/list/type/{type}")
    public Result<List<Activity>> getActivityListByType(@PathVariable Integer type) {
        try {
            List<Activity> activities = activityService.findByType(type);
            return success(activities);
        } catch (Exception e) {
            e.printStackTrace();
            return error("获取活动列表失败: " + e.getMessage());
        }
    }

    /** 创建新活动 */
    @PostMapping("/create")
    public Result<Activity> createActivity(@RequestBody Activity activity) {
        try {
            activityService.save(activity);
            return success(activity);
        } catch (Exception e) {
            e.printStackTrace();
            return error("创建活动失败: " + e.getMessage());
        }
    }

    /** 更新活动信息 */
    @PostMapping("/{id}/update")
    public Result<Activity> updateActivity(@PathVariable Long id, @RequestBody Activity activity) {
        try {
            activity.setId(id);
            activityService.save(activity);
            return success(activity);
        } catch (Exception e) {
            e.printStackTrace();
            return error("更新活动失败: " + e.getMessage());
        }
    }

    /** 删除活动 */
    @PostMapping("/{id}/delete")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        try {
            activityService.delete(id);
            return success(null);
        } catch (Exception e) {
            e.printStackTrace();
            return error("删除活动失败: " + e.getMessage());
        }
    }

    /** 报名参加活动 */
    @PostMapping("/{id}/join")
    public Result<Void> joinActivity(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(String.valueOf(body.get("userId")));
            String participantName = (String) body.get("participantName");
            String participantPhone = (String) body.get("participantPhone");
            activityService.joinActivity(id, userId, participantName, participantPhone);
            return success(null);
        } catch (Exception e) {
            e.printStackTrace();
            return error("报名失败: " + e.getMessage());
        }
    }

    /** 取消活动报名 */
    @PostMapping("/{id}/cancel-join")
    public Result<Void> cancelJoin(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        try {
            activityService.cancelJoin(id, body.get("userId"));
            return success(null);
        } catch (Exception e) {
            e.printStackTrace();
            return error("取消报名失败: " + e.getMessage());
        }
    }

    /** 分页查询活动参与者列表，可按状态筛选 */
    @GetMapping("/{id}/participants")
    public Result<Map<String, Object>> getActivityParticipants(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        try {
            // 查询参与者列表
            List<ActivityParticipant> participants = activityService.findParticipantsByActivityId(id);

            // 根据状态筛选
            if (status != null) {
                participants = participants.stream()
                        .filter(p -> p.getStatus() == status)
                        .collect(Collectors.toList());
            }

            // 计算偏移量
            int offset = (page - 1) * size;

            // 分页处理
            int total = participants.size();
            int end = Math.min(offset + size, total);
            List<ActivityParticipant> paginatedParticipants;
            if (offset < end) {
                paginatedParticipants = participants.subList(offset, end);
            } else {
                paginatedParticipants = new ArrayList<>();
            }

            // 构建分页响应
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("records", paginatedParticipants);
            pageData.put("total", total);
            pageData.put("current", page);
            pageData.put("size", size);
            pageData.put("pages", (int) Math.ceil((double) total / size));

            return success(pageData);
        } catch (Exception e) {
            e.printStackTrace();
            return error("获取参与者列表失败: " + e.getMessage());
        }
    }

    /** 获取用户已报名的活动列表 */
    @GetMapping("/joined")
    public Result<List<Activity>> getJoinedActivities(@RequestParam Long userId) {
        try {
            List<Activity> activities = activityService.findJoinedActivities(userId);
            return success(activities);
        } catch (Exception e) {
            e.printStackTrace();
            return error("获取已报名活动列表失败：" + e.getMessage());
        }
    }

    /** 判断指定用户是否已参与某活动 */
    @GetMapping("/{id}/joined")
    public Result<Boolean> checkIfJoined(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                return success(false);
            }
            QueryWrapper<ActivityParticipant> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("activity_id", id).eq("user_id", userId);
            ActivityParticipant participant = activityService.findParticipantsByActivityId(id).stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
            return success(participant != null);
        } catch (Exception e) {
            e.printStackTrace();
            return error("检查参与状态失败：" + e.getMessage());
        }
    }

    /** 获取首页推荐的进行中活动列表（按推荐排序） */
    @GetMapping("/recommended")
    public Result<Map<String, Object>> getRecommendedActivities(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "6") Integer size) {
        try {
            // 查询推荐活动列表（按推荐排序）
            List<Activity> activities = activityService.findRecommendedActivities();

            // 只返回进行中的活动
            activities = activities.stream()
                    .filter(a -> a.getStatus() == 1) // 状态为进行中
                    .sorted((a1, a2) -> {
                        // 按推荐排序
                        Integer order1 = a1.getRecommendOrder() != null ? a1.getRecommendOrder() : 999;
                        Integer order2 = a2.getRecommendOrder() != null ? a2.getRecommendOrder() : 999;
                        return order1.compareTo(order2);
                    })
                    .collect(Collectors.toList());

            // 分页处理
            int total = activities.size();
            int offset = (page - 1) * size;
            int end = Math.min(offset + size, total);
            List<Activity> paginatedActivities = offset < end ? activities.subList(offset, end) : new ArrayList<>();

            // 构建分页响应
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("records", paginatedActivities);
            pageData.put("total", total);
            pageData.put("current", page);
            pageData.put("size", size);
            pageData.put("pages", (int) Math.ceil((double) total / size));

            return success(pageData);
        } catch (Exception e) {
            e.printStackTrace();
            return error("获取推荐活动列表失败：" + e.getMessage());
        }
    }

    /** 更新活动状态（如进行中/已结束） */
    @PostMapping("/{id}/update-status")
    public Result<Void> updateActivityStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            activityService.updateStatus(id, body.get("status"));
            return success(null);
        } catch (Exception e) {
            e.printStackTrace();
            return error("更新活动状态失败: " + e.getMessage());
        }
    }

    /** 更新活动参与者状态 */
    @PostMapping("/participant/{id}/update-status")
    public Result<Void> updateParticipantStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            activityService.updateParticipantStatus(id, body.get("status"));
            return success(null);
        } catch (Exception e) {
            e.printStackTrace();
            return error("更新参与者状态失败: " + e.getMessage());
        }
    }
}
