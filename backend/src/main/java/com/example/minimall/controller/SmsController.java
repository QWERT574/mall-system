package com.example.minimall.controller;

import com.example.minimall.annotation.RateLimit;
import com.example.minimall.common.Result;
import com.example.minimall.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信验证码接口（公开接口,无需登录）
 * - POST /api/sms/send   body: { phone: "13800138000" }
 *   返回: { code:0, message:"...", data: { devCode: "1234" } }
 *         dev 模式下 data.devCode 直接返回验证码,方便测试
 * - POST /api/sms/verify body: { phone, code }
 *   返回: { code:0, message:"验证通过" } / 各种错误
 */
@RestController
@RequestMapping("/api/sms")
public class SmsController extends BaseController {

    @Autowired
    private SmsService smsService;

    @RateLimit(limit = 3, timeout = 60)
    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        Map<String, Object> data = smsService.sendCode(phone);

        if (Boolean.TRUE.equals(data.get("ok"))) {
            return success(data);
        } else {
            return error((String) data.get("message"));
        }
    }

    @PostMapping("/verify")
    public Result<Void> verify(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");
        int rc = smsService.verifyCode(phone, code);
        switch (rc) {
            case SmsService.VERIFY_OK:
                return success(null);
            case SmsService.VERIFY_EMPTY:
                return error("请先获取验证码");
            case SmsService.VERIFY_EXPIRED:
                return error("验证码已过期,请重新获取");
            case SmsService.VERIFY_WRONG:
                return error("验证码错误");
            default:
                return error("验证失败");
        }
    }
}
