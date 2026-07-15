package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.model.AIServiceLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 服务调用日志 Mapper，对应 ai_service_log 表
 */
@Mapper
public interface AIServiceLogMapper extends BaseMapper<AIServiceLog> {
    /** 显式声明 insert 方法，确保使用自定义 SQL */
    @Override
    int insert(AIServiceLog entity);

    /** 根据用户 ID 查询 AI 服务日志 */
    List<AIServiceLog> selectByUserId(Long userId);

    /** 分页查询 AI 服务日志 */
    IPage<AIServiceLog> selectPage(Page<AIServiceLog> page, @Param("userId") Long userId, @Param("serviceType") Integer serviceType);

    /** 根据用户 ID 删除 AI 服务日志 */
    void deleteByUserId(@Param("userId") Long userId);

    /** 根据服务类型删除 AI 服务日志 */
    void deleteByServiceType(@Param("serviceType") Integer serviceType);

    /** 删除所有 AI 服务日志 */
    void deleteAll();
}
