package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录表 Mapper，对应 payment 表
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}