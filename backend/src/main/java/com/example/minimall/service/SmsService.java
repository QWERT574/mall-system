package com.example.minimall.service;

import java.util.Map;

/** 短信验证码服务接口 */
public interface SmsService {
    int VERIFY_OK = 0;
    int VERIFY_EMPTY = 1;
    int VERIFY_EXPIRED = 2;
    int VERIFY_WRONG = 3;

    /** 发送验证码 */
    Map<String, Object> sendCode(String phone);
    /** 校验验证码 */
    int verifyCode(String phone, String code);
}
