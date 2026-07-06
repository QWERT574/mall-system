package com.example.minimall.service;

import com.example.minimall.mapper.LogisticsMapper;
import com.example.minimall.mapper.LogisticsTraceMapper;
import com.example.minimall.model.Logistics;
import com.example.minimall.model.LogisticsTrace;
import com.example.minimall.utils.LogisticsApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** 物流信息与物流轨迹服务，支持第三方物流实时同步 */
@Service
public class LogisticsService {

    /** 物流主表 Mapper */
    @Autowired
    private LogisticsMapper logisticsMapper;

    /** 物流轨迹 Mapper */
    @Autowired
    private LogisticsTraceMapper logisticsTraceMapper;

    /** 第三方物流 API 客户端 */
    @Autowired
    private LogisticsApiClient logisticsApiClient;

    /**
     * 根据物流主键 ID 查询
     *
     * @param id 物流记录主键
     * @return 物流实体，未找到返回 null
     */
    public Logistics findById(Long id) {
        return logisticsMapper.selectById(id);
    }

    /**
     * 根据订单 ID 查询关联的物流信息（**一个订单对应一条物流**）
     *
     * @param orderId 订单 ID
     * @return 物流实体
     */
    public Logistics findByOrderId(Long orderId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Logistics> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        return logisticsMapper.selectOne(queryWrapper);
    }

    /**
     * 查询物流的全部轨迹记录（**按时间顺序**）
     *
     * @param logisticsId 物流主键
     * @return 轨迹列表
     */
    public List<LogisticsTrace> findTraceByLogisticsId(Long logisticsId) {
        return logisticsTraceMapper.selectByLogisticsId(logisticsId);
    }

    /**
     * 更新物流主表（**自动刷新最后更新时间**）
     *
     * @param logistics 物流实体（带主键）
     */
    public void updateLogistics(Logistics logistics) {
        logistics.setLastUpdateTime(LocalDateTime.now());
        logisticsMapper.updateById(logistics);
    }

    /**
     * 新增一条物流轨迹
     * <p>同时写入 updateTime / createdAt / updatedAt 三个时间字段</p>
     *
     * @param logisticsId 物流主键
     * @param status      状态描述（如"已揽件"、"运输中"、"派送中"等）
     * @param description 详细描述
     * @param location    当前所在位置
     */
    public void addLogisticsTrace(Long logisticsId, String status, String description, String location) {
        LogisticsTrace trace = new LogisticsTrace();
        trace.setLogisticsId(logisticsId);
        trace.setStatus(status);
        trace.setDescription(description);
        trace.setLocation(location);
        trace.setUpdateTime(LocalDateTime.now());
        trace.setCreatedAt(LocalDateTime.now());
        trace.setUpdatedAt(LocalDateTime.now());
        logisticsTraceMapper.insert(trace);
    }

    /**
     * 同步第三方物流信息（**核心方法**，会被定时任务调用）
     * <p>
     * 流程：
     * <ol>
     *   <li>查物流主表</li>
     *   <li>调第三方 API 拉取最新轨迹</li>
     *   <li>清空旧轨迹</li>
     *   <li>写入新轨迹（每条带尝试解析时间戳，失败时使用当前时间）</li>
     *   <li>刷新主表 lastUpdateTime</li>
     * </ol>
     * </p>
     *
     * @param logisticsId 物流记录主键
     */
    public void syncThirdPartyLogistics(Long logisticsId) {
        Logistics logistics = logisticsMapper.selectById(logisticsId);
        if (logistics != null) {
            // 调用第三方物流API获取实时物流信息
            List<Map<String, Object>> logisticsInfo = logisticsApiClient.getLogisticsInfo(
                logistics.getLogisticsCompany(), logistics.getLogisticsNo());
            
            // 清空旧的物流轨迹
            logisticsTraceMapper.deleteByLogisticsId(logisticsId);
            
            // 添加新的物流轨迹
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Map<String, Object> traceInfo : logisticsInfo) {
                String status = String.valueOf(traceInfo.getOrDefault("status", ""));
                String description = String.valueOf(traceInfo.getOrDefault("description", ""));
                String location = String.valueOf(traceInfo.getOrDefault("location", ""));
                String updateTimeStr = String.valueOf(traceInfo.getOrDefault("updateTime", LocalDateTime.now().format(formatter)));
                
                LocalDateTime updateTime;
                try {
                    updateTime = LocalDateTime.parse(updateTimeStr, formatter);
                } catch (Exception e) {
                    updateTime = LocalDateTime.now();
                }
                
                LogisticsTrace trace = new LogisticsTrace();
                trace.setLogisticsId(logisticsId);
                trace.setStatus(status);
                trace.setDescription(description);
                trace.setLocation(location);
                trace.setUpdateTime(updateTime);
                trace.setCreatedAt(LocalDateTime.now());
                trace.setUpdatedAt(LocalDateTime.now());
                logisticsTraceMapper.insert(trace);
            }
            
            // 更新物流信息的最后更新时间
            logistics.setLastUpdateTime(LocalDateTime.now());
            logisticsMapper.updateById(logistics);
        }
    }
}