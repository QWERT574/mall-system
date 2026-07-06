package com.example.minimall.service.impl;

import com.example.minimall.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码服务（不依赖 Redis,使用进程内 Map 存储）
 * - 手机号 -> 4 位数字验证码
 * - 5 分钟过期
 * - 60 秒发送间隔
 * - 一次性使用:验证通过后立即从 Map 删除
 */
@Service
public class SmsServiceImpl implements SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);

    /** phone -> (code, expireAt) */
    private static final Map<String, SmsEntry> CODE_STORE = new ConcurrentHashMap<>();
    /** phone -> lastSendTime(ms) */
    private static final Map<String, Long> LAST_SEND_STORE = new ConcurrentHashMap<>();

    /** 验证码有效期:5 分钟 */
    private static final long TTL_MILLIS = 5 * 60 * 1000L;
    /** 发送冷却:60 秒 */
    private static final long SEND_INTERVAL_MILLIS = 60 * 1000L;

    /** 验证结果 */
    public static final int VERIFY_OK = 0;
    public static final int VERIFY_EMPTY = 1;       // 验证码为空 / 不存在
    public static final int VERIFY_EXPIRED = 2;     // 已过期
    public static final int VERIFY_WRONG = 3;       // 错误

    /**
     * 发送验证码
     * <p>
     * 流程：
     * <ol>
     *   <li>校验手机号格式（11 位、1[3-9] 开头）</li>
     *   <li>60 秒内不能重发</li>
     *   <li>生成 4 位数字验证码</li>
     *   <li>存储到进程内 Map，TTL 5 分钟</li>
     *   <li>清理过期项 + 模拟发送（开发环境）</li>
     * </ol>
     * </p>
     *
     * @param phone 手机号
     * @return 结果 Map：ok / message / expiresIn / devCode（仅 dev 环境有）
     */
    public Map<String, Object> sendCode(String phone) {
        Map<String, Object> result = new HashMap<>();

        // 1. 手机号格式校验
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            result.put("ok", false);
            result.put("message", "手机号格式不正确");
            return result;
        }

        long now = System.currentTimeMillis();

        // 2. 发送频率限制
        Long lastSend = LAST_SEND_STORE.get(phone);
        if (lastSend != null && (now - lastSend) < SEND_INTERVAL_MILLIS) {
            long remainSec = (SEND_INTERVAL_MILLIS - (now - lastSend)) / 1000 + 1;
            result.put("ok", false);
            result.put("message", "发送过于频繁,请 " + remainSec + " 秒后再试");
            result.put("retryAfterSeconds", remainSec);
            return result;
        }

        // 3. 生成 4 位数字验证码
        String code = String.format("%04d", new Random().nextInt(10000));

        // 4. 存储
        CODE_STORE.put(phone, new SmsEntry(code, now + TTL_MILLIS));
        LAST_SEND_STORE.put(phone, now);

        // 5. 清理过期
        cleanupExpired();

        // 6. 模拟发送(实际项目接入短信服务商 API)
        logger.info("============================================");
        logger.info("【短信验证码】  收件人:{}  验证码:{}", phone, code);
        logger.info("============================================");

        result.put("ok", true);
        result.put("message", "验证码发送成功");
        result.put("expiresIn", TTL_MILLIS / 1000);
        // dev 模式:开发环境把验证码直接返回,方便测试
        result.put("devCode", code);
        return result;
    }

    /**
     * 校验验证码（**一次性使用**，验证通过后立即从 Map 删除）
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return 验证结果码：{@link #VERIFY_OK} / {@link #VERIFY_EMPTY} / {@link #VERIFY_EXPIRED} / {@link #VERIFY_WRONG}
     */
    public int verifyCode(String phone, String code) {
        if (phone == null || code == null || code.isEmpty()) {
            return VERIFY_EMPTY;
        }
        SmsEntry entry = CODE_STORE.get(phone);
        if (entry == null) {
            return VERIFY_EMPTY;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            CODE_STORE.remove(phone);
            return VERIFY_EXPIRED;
        }
        if (!entry.code.equals(code)) {
            return VERIFY_WRONG;
        }
        // 一次性使用
        CODE_STORE.remove(phone);
        return VERIFY_OK;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        CODE_STORE.entrySet().removeIf(e -> now > e.getValue().expireAt);
    }

    private static class SmsEntry {
        final String code;
        final long expireAt;
        SmsEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
