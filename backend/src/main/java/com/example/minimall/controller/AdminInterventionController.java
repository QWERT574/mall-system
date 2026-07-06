package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.minimall.common.Result;
import com.example.minimall.model.AdminIntervention;
import com.example.minimall.service.AdminInterventionService;
import com.example.minimall.vo.AdminInterventionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import static com.example.minimall.vo.Converters.convert;
import static com.example.minimall.vo.Converters.convertList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 管理员介入申请（纠纷仲裁）相关接口 */
@RestController
@RequestMapping("/api/admin/intervention")
public class AdminInterventionController {

    private static final Logger logger = LoggerFactory.getLogger(AdminInterventionController.class);

    /** 介入申请业务服务 */
    @Autowired
    private AdminInterventionService adminInterventionService;

    /** WebSocket 消息推送模板（可空） */
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    /** 创建介入申请 */
    @PostMapping
    public Result<AdminInterventionVO> createIntervention(@RequestBody AdminIntervention intervention) {
        try {
            AdminIntervention created = adminInterventionService.createIntervention(intervention);
            return Result.success(convert(created, AdminInterventionVO::new));
        } catch (Exception e) {
            logger.error("创建介入申请失败", e);
            return Result.error("创建介入申请失败：" + e.getMessage());
        }
    }

    /** 根据 ID 查询介入申请详情 */
    @GetMapping("/{id}")
    public Result<AdminInterventionVO> getInterventionById(@PathVariable Long id) {
        try {
            AdminIntervention intervention = adminInterventionService.getInterventionById(id);
            if (intervention != null) {
                return Result.success(convert(intervention, AdminInterventionVO::new));
            } else {
                return Result.error("申请不存在");
            }
        } catch (Exception e) {
            logger.error("获取申请详情失败", e);
            return Result.error("获取申请详情失败：" + e.getMessage());
        }
    }

    /** 查询指定用户提交的所有介入申请 */
    @GetMapping("/user/{userId}")
    public Result<List<AdminInterventionVO>> getInterventionsByUserId(@PathVariable Long userId) {
        try {
            List<AdminIntervention> interventions = adminInterventionService.getInterventionsByUserId(userId);
            return Result.success(convertList(interventions, AdminInterventionVO::new));
        } catch (Exception e) {
            logger.error("获取用户申请列表失败", e);
            return Result.error("获取用户申请列表失败：" + e.getMessage());
        }
    }

    /** 查询指定商家相关的所有介入申请 */
    @GetMapping("/seller/{sellerId}")
    public Result<List<AdminInterventionVO>> getInterventionsBySellerId(@PathVariable Long sellerId) {
        try {
            List<AdminIntervention> interventions = adminInterventionService.getInterventionsBySellerId(sellerId);
            return Result.success(convertList(interventions, AdminInterventionVO::new));
        } catch (Exception e) {
            logger.error("获取商家申请列表失败", e);
            return Result.error("获取商家申请列表失败：" + e.getMessage());
        }
    }

    /** 分页查询介入申请，可按状态筛选 */
    @GetMapping
    public Result<Map<String, Object>> getInterventionsPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        try {
            IPage<AdminIntervention> result = adminInterventionService.getInterventionsPage(page, size, status);

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("total", result.getTotal());
            pageInfo.put("records", convertList(result.getRecords(), AdminInterventionVO::new));
            pageInfo.put("current", result.getCurrent());
            pageInfo.put("size", result.getSize());

            return Result.success(pageInfo);
        } catch (Exception e) {
            logger.error("获取申请列表失败", e);
            return Result.error("获取申请列表失败：" + e.getMessage());
        }
    }

    /** 获取介入申请相关统计数据 */
    @PostMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = adminInterventionService.getInterventionStats();
            return Result.success(stats);
        } catch (Exception e) {
            logger.error("获取统计数据失败", e);
            return Result.error("获取统计数据失败：" + e.getMessage());
        }
    }

    /** 分页查询待处理的介入申请 */
    @GetMapping("/pending")
    public Result<Map<String, Object>> getPendingInterventions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            IPage<AdminIntervention> result = adminInterventionService.getPendingInterventionsPage(page, size);

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("total", result.getTotal());
            pageInfo.put("records", convertList(result.getRecords(), AdminInterventionVO::new));
            pageInfo.put("current", result.getCurrent());
            pageInfo.put("size", result.getSize());

            return Result.success(pageInfo);
        } catch (Exception e) {
            logger.error("获取待处理申请列表失败", e);
            return Result.error("获取待处理申请列表失败：" + e.getMessage());
        }
    }

    /** 分配管理员处理介入申请 */
    @PostMapping("/{id}/assign")
    public Result<AdminInterventionVO> assignAdmin(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Long adminId = request.get("adminId") != null ?
                Long.valueOf(request.get("adminId").toString()) : null;
            AdminIntervention intervention = adminInterventionService.assignAdmin(id, adminId);
            return Result.success(convert(intervention, AdminInterventionVO::new));
        } catch (Exception e) {
            logger.error("分配管理员失败", e);
            return Result.error("分配管理员失败：" + e.getMessage());
        }
    }

    /** 管理员处理介入申请：修改状态/备注，并通过 WebSocket 通知买卖双方 */
    @PostMapping("/{id}/process")
    public Result<AdminInterventionVO> processIntervention(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Integer status = (Integer) request.get("status");
            String remark = (String) request.getOrDefault("remark", request.get("adminRemark"));
            Long adminId = request.get("adminId") != null ?
                Long.valueOf(request.get("adminId").toString()) : null;

            AdminIntervention intervention = adminInterventionService.processIntervention(id, status, remark, adminId);

            if (intervention.getSessionId() != null) {
                try {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "admin_intervened");
                    notification.put("interventionId", intervention.getId());
                    notification.put("sessionId", intervention.getSessionId());
                    notification.put("status", status);
                    notification.put("remark", remark);

                    if (messagingTemplate != null) {
                        if (intervention.getUserId() != null) {
                            messagingTemplate.convertAndSend("/topic/chat/user/" + intervention.getUserId(), notification);
                            logger.info("Admin intervention notification sent to user: {}", intervention.getUserId());
                        }

                        if (intervention.getSellerId() != null) {
                            messagingTemplate.convertAndSend("/topic/chat/seller/" + intervention.getSellerId(), notification);
                            logger.info("Admin intervention notification sent to seller: {}", intervention.getSellerId());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to send intervention notification via WebSocket", e);
                }
            }

            return Result.success(convert(intervention, AdminInterventionVO::new));
        } catch (Exception e) {
            logger.error("处理申请失败", e);
            return Result.error("处理申请失败：" + e.getMessage());
        }
    }

    /** 提交仲裁最终结论（支持退款/罚款） */
    @PostMapping("/{id}/submit")
    public Result<AdminInterventionVO> submitDecision(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Integer decision = request.get("decision") != null ?
                Integer.valueOf(request.get("decision").toString()) : null;
            String reason = (String) request.get("reason");
            Double refundAmount = request.get("refundAmount") != null ?
                Double.valueOf(request.get("refundAmount").toString()) : null;
            String penalty = (String) request.get("penalty");
            Double fineAmount = request.get("fineAmount") != null ?
                Double.valueOf(request.get("fineAmount").toString()) : null;
            String evidenceImages = (String) request.get("evidenceImages");

            Map<String, Object> processParams = new HashMap<>();
            processParams.put("status", decision);
            processParams.put("remark", reason);
            if (refundAmount != null) processParams.put("refundAmount", refundAmount);
            if (penalty != null) processParams.put("penalty", penalty);
            if (fineAmount != null) processParams.put("fineAmount", fineAmount);
            if (evidenceImages != null) processParams.put("evidenceImages", evidenceImages);

            AdminIntervention intervention = adminInterventionService.submitDecision(id, processParams);

            if (intervention.getSessionId() != null && messagingTemplate != null) {
                try {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "admin_intervened");
                    notification.put("interventionId", id);
                    notification.put("sessionId", intervention.getSessionId());
                    notification.put("status", decision);
                    notification.put("remark", reason);
                    if (intervention.getUserId() != null) {
                        messagingTemplate.convertAndSend("/topic/chat/user/" + intervention.getUserId(), notification);
                    }
                    if (intervention.getSellerId() != null) {
                        messagingTemplate.convertAndSend("/topic/chat/seller/" + intervention.getSellerId(), notification);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to send intervention notification via WebSocket", e);
                }
            }

            return Result.success(convert(intervention, AdminInterventionVO::new));
        } catch (Exception e) {
            logger.error("提交仲裁结果失败", e);
            return Result.error("提交仲裁结果失败：" + e.getMessage());
        }
    }

    /** 更新介入申请信息 */
    @PutMapping("/{id}")
    public Result<AdminInterventionVO> updateIntervention(
            @PathVariable Long id,
            @RequestBody AdminIntervention intervention) {
        try {
            AdminIntervention updated = adminInterventionService.updateIntervention(id, intervention);
            return Result.success(convert(updated, AdminInterventionVO::new));
        } catch (Exception e) {
            logger.error("更新申请失败", e);
            return Result.error("更新申请失败：" + e.getMessage());
        }
    }

    /** 删除介入申请 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteIntervention(@PathVariable Long id) {
        try {
            adminInterventionService.deleteIntervention(id);
            return Result.success(null);
        } catch (Exception e) {
            logger.error("删除申请失败", e);
            return Result.error("删除申请失败：" + e.getMessage());
        }
    }
}
