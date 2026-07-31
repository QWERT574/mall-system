package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.ShippingAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家发货地址表 Mapper，对应 shipping_address 表
 */
@Mapper
public interface ShippingAddressMapper extends BaseMapper<ShippingAddress> {
    /** 查询默认发货地址 */
    ShippingAddress selectDefault();
}
