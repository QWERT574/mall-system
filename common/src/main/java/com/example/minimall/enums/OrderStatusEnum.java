package com.example.minimall.enums;

/**
 * 订单状态枚举，描述 orders 表中 status 字段的取值（待付款/已付款/已发货/已完成/已取消）
 */
public enum OrderStatusEnum {
    UNPAID(0, "待付款"),
    PAID(1, "已付款"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELED(4, "已取消");
    
    private final Integer code;
    private final String desc;
    
    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public static OrderStatusEnum getByCode(Integer code) {
        for (OrderStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
