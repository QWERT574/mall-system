package com.example.minimall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.minimall.model.AdminIntervention;

import java.util.List;
import java.util.Map;

/** 管理员介入申请服务接口 */
public interface AdminInterventionService {

    /** 创建介入申请 */
    AdminIntervention createIntervention(AdminIntervention intervention);

    /** 根据 ID 获取介入申请 */
    AdminIntervention getInterventionById(Long id);

    /** 获取指定用户的介入申请列表 */
    List<AdminIntervention> getInterventionsByUserId(Long userId);

    /** 获取指定商家的介入申请列表 */
    List<AdminIntervention> getInterventionsBySellerId(Long sellerId);

    /** 分页获取介入申请 */
    IPage<AdminIntervention> getInterventionsPage(Integer page, Integer size, Integer status);

    /** 处理介入申请 */
    AdminIntervention processIntervention(Long id, Integer status, String remark, Long adminId);

    /** 提交仲裁决策 */
    AdminIntervention submitDecision(Long id, Map<String, Object> params);

    /** 更新介入申请 */
    AdminIntervention updateIntervention(Long id, AdminIntervention intervention);

    /** 删除介入申请 */
    void deleteIntervention(Long id);

    /** 获取待处理的介入申请 */
    List<AdminIntervention> getPendingInterventions();

    /** 分页获取待处理的介入申请 */
    IPage<AdminIntervention> getPendingInterventionsPage(Integer page, Integer size);

    /** 指派管理员 */
    AdminIntervention assignAdmin(Long id, Long adminId);

    /** 获取介入申请统计数据 */
    Map<String, Object> getInterventionStats();

    /** 获取待处理数量 */
    long getPendingCount();

    /** 获取处理中数量 */
    long getProcessingCount();

    /** 获取已完成数量 */
    long getCompletedCount();
}
