package com.example.minimall.enums;

/**
 * 用户优惠券状态枚举，描述 user_coupon 表中 status 字段的取值
 */
public enum UserCouponStatusEnum {
    UNUSED(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期");

    private final Integer code;
    private final String desc;

    UserCouponStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }

    public static UserCouponStatusEnum getByCode(Integer code) {
        for (UserCouponStatusEnum s : values()) {
            if (s.getCode().equals(code)) return s;
        }
        return UNUSED;
    }
}
