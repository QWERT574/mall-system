package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ServiceRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 售后服务流转记录 Mapper，对应 service_record 表
 */
@Mapper
public interface ServiceRecordMapper extends BaseMapper<ServiceRecord> {
    /** 根据售后服务 ID 查询流转记录 */
    List<ServiceRecord> selectByAfterSaleId(Long afterSaleId);
}
