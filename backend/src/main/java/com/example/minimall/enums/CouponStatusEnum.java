package com.example.minimall.enums;

/**
 * 优惠券状态枚举，描述 coupon 表中 status 字段的取值
 */
public enum CouponStatusEnum {
    ACTIVE(1, "进行中"),
    INACTIVE(0, "已停用");

    private final Integer code;
    private final String desc;

    CouponStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }

    public static CouponStatusEnum getByCode(Integer code) {
        for (CouponStatusEnum s : values()) {
            if (s.getCode().equals(code)) return s;
        }
        return INACTIVE;
    }
}
