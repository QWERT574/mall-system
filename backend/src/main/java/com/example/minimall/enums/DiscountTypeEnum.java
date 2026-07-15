package com.example.minimall.enums;

/**
 * 折扣活动类型枚举，描述 discount_activity 表中 type 字段的取值（满减/折扣/秒杀）
 */
public enum DiscountTypeEnum {
    FULL_REDUCTION(1, "满减"),
    DISCOUNT(2, "限时折扣"),
    FLASH_SALE(3, "秒杀");

    private final Integer code;
    private final String desc;

    DiscountTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }

    public static DiscountTypeEnum getByCode(Integer code) {
        for (DiscountTypeEnum t : values()) {
            if (t.getCode().equals(code)) return t;
        }
        return DISCOUNT;
    }
}
