package com.example.minimall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.mapper.AIServiceLogMapper;
import com.example.minimall.model.AIServiceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 服务调用日志管理服务。
 *
 * <p>负责 ai_service_log 表的 CRUD 操作，从 {@link AIService} 中提取以分离日志管理职责。</p>
 */
@Service
public class AiLogService {
    private static final Logger logger = LoggerFactory.getLogger(AiLogService.class);
    private final AIServiceLogMapper aiServiceLogMapper;

    public AiLogService(AIServiceLogMapper aiServiceLogMapper) {
        this.aiServiceLogMapper = aiServiceLogMapper;
    }

    /** 查询用户的所有 AI 服务日志 */
    public List<AIServiceLog> getLogsByUserId(Long userId) {
        return aiServiceLogMapper.selectByUserId(userId);
    }

    /** 分页查询 AI 服务日志（管理后台用） */
    public IPage<AIServiceLog> getLogsPage(int page, int size, Long userId, Integer serviceType) {
        Page<AIServiceLog> pageQuery = new Page<>(page, size);
        return aiServiceLogMapper.selectPage(pageQuery, userId, serviceType);
    }

    /** 根据 ID 查询 AI 服务日志详情 */
    public AIServiceLog getLogById(Long id) {
        return aiServiceLogMapper.selectById(id);
    }

    /** 删除单条 AI 服务日志 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLog(Long id) {
        aiServiceLogMapper.deleteById(id);
    }

    /** 批量删除 AI 服务日志 */
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteLogs(List<Long> ids) {
        for (Long id : ids) {
            aiServiceLogMapper.deleteById(id);
        }
    }

    /** 清空全部 AI 服务日志 */
    @Transactional(rollbackFor = Exception.class)
    public void clearLogs() {
        aiServiceLogMapper.deleteAll();
    }

    /** 清空指定用户的 AI 服务日志 */
    @Transactional(rollbackFor = Exception.class)
    public void clearLogsByUserId(Long userId) {
        aiServiceLogMapper.deleteByUserId(userId);
    }

    /** 清空指定类型的 AI 服务日志 */
    @Transactional(rollbackFor = Exception.class)
    public void clearLogsByServiceType(Integer serviceType) {
        aiServiceLogMapper.deleteByServiceType(serviceType);
    }
}
