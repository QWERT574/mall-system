package com.example.minimall.enums;

/**
 * 用户类型枚举，用于区分消费者、供应商和管理员三种角色
 */
public enum UserTypeEnum {
    CONSUMER(0, "消费者", "购买农产品的企业或个人"),
    SUPPLIER(1, "供应商", "农产品提供方（乡村或个人）"),
    ADMIN(2, "管理员", "平台管理员，负责用户管理和系统运维");
    
    private final Integer code;
    private final String name;
    private final String description;
    
    UserTypeEnum(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static UserTypeEnum getByCode(Integer code) {
        for (UserTypeEnum typeEnum : values()) {
            if (typeEnum.getCode().equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }
    
    public static UserTypeEnum getByName(String name) {
        for (UserTypeEnum typeEnum : values()) {
            if (typeEnum.getName().equals(name)) {
                return typeEnum;
            }
        }
        return null;
    }
}
