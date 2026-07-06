package com.example.minimall.enums;

/**
 * 优惠券类型枚举，描述 coupon 表中 type 字段的取值（满减券/折扣券）
 */
public enum CouponTypeEnum {
    FULL_REDUCTION(1, "满减券"),
    DISCOUNT(2, "折扣券");

    private final Integer code;
    private final String desc;

    CouponTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }

    public static CouponTypeEnum getByCode(Integer code) {
        for (CouponTypeEnum type : values()) {
            if (type.getCode().equals(code)) return type;
        }
        return FULL_REDUCTION;
    }
}
