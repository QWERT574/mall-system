package com.example.minimall.service;

import com.example.minimall.model.SystemConfig;

import java.util.List;

/** 系统配置服务接口 */
public interface SystemConfigService {
    /** 根据 key 获取配置值 */
    String getConfigValue(String key);
    /** 获取所有配置 */
    List<SystemConfig> listAll();
    /** 根据 key 获取配置项 */
    SystemConfig findByKey(String key);
    /** 保存配置项 */
    SystemConfig save(SystemConfig config);
    /** 根据 ID 删除配置项 */
    void delete(Long id);
}
