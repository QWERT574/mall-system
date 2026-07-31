package com.example.minimall.enums;

/**
 * 售后服务状态枚举，描述 after_sale_service 表中 status 字段的取值
 */
public enum AfterSaleStatusEnum {
    PENDING(0, "待处理", "等待商家处理"),
    PROCESSING(1, "处理中", "商家正在处理中"),
    RESOLVED(2, "已解决", "售后已完结"),
    CLOSED(3, "已关闭", "售后申请已关闭");

    private final Integer code;
    private final String desc;
    private final String remark;

    AfterSaleStatusEnum(Integer code, String desc, String remark) {
        this.code = code;
        this.desc = desc;
        this.remark = remark;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public String getRemark() {
        return remark;
    }

    public static AfterSaleStatusEnum getByCode(Integer code) {
        for (AfterSaleStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }

    public static boolean isValidTransition(Integer currentStatus, Integer targetStatus) {
        if (currentStatus == null) return true;
        if (currentStatus == 0 && (targetStatus == 1 || targetStatus == 2 || targetStatus == 3)) return true;
        if (currentStatus == 1 && (targetStatus == 2 || targetStatus == 3)) return true;
        return currentStatus.equals(targetStatus);
    }
}
