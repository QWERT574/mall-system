package com.example.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.SystemConfigMapper;
import com.example.minimall.model.SystemConfig;
import com.example.minimall.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 系统配置服务实现 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    /** 系统配置Mapper */
    private final SystemConfigMapper mapper;

    public SystemConfigServiceImpl(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 根据配置键读取配置值（**字符串**）
     * <p>通常用于运行时根据 key 拉配置；找不到时返回 null</p>
     *
     * @param key 配置键
     * @return 配置值
     */
    public String getConfigValue(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        String value = mapper.getConfigValue(key);
        return value;
    }

    /**
     * 获取全部配置项
     *
     * @return 配置列表
     */
    public List<SystemConfig> listAll() {
        return mapper.selectList(null);
    }

    /**
     * 根据配置键查询配置实体
     *
     * @param key 配置键
     * @return 配置实体
     */
    public SystemConfig findByKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        QueryWrapper<SystemConfig> qw = new QueryWrapper<>();
        qw.eq("config_key", key);
        return mapper.selectOne(qw);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 新增或更新配置项（**已存在则更新**，事务）
     *
     * @param config 配置实体
     * @return 保存后的实体
     */
    public SystemConfig save(SystemConfig config) {
        if (config.getConfigKey() == null || config.getConfigKey().trim().isEmpty()) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        if (config.getConfigValue() == null) {
            throw new IllegalArgumentException("配置值不能为空");
        }
        QueryWrapper<SystemConfig> qw = new QueryWrapper<>();
        qw.eq("config_key", config.getConfigKey());
        SystemConfig existing = mapper.selectOne(qw);
        if (existing != null) {
            existing.setConfigValue(config.getConfigValue());
            existing.setDescription(config.getDescription());
            mapper.updateById(existing);
            return existing;
        } else {
            mapper.insert(config);
            return config;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    /** 根据 ID 删除配置 */
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("配置ID不能为空");
        }
        mapper.deleteById(id);
    }
}
