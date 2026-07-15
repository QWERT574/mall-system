package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.AfterSaleService;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 售后服务表 Mapper，对应 after_sale_service 表
 */
@Mapper
public interface AfterSaleServiceMapper extends BaseMapper<AfterSaleService> {
    /** 根据用户 ID 查询售后服务列表 */
    List<AfterSaleService> selectByUserId(@Param("userId") Long userId);

    /** 根据订单 ID 查询售后服务列表 */
    List<AfterSaleService> selectByOrderId(@Param("orderId") Long orderId);

    /** 根据商品 ID 查询售后服务列表 */
    List<AfterSaleService> selectByProductId(@Param("productId") Long productId);
}
