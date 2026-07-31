package com.example.minimall.service.impl;

import com.example.minimall.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务（C4：从进程内 Map 迁移到 Redis）
 *
 * <p>存储方案：
 * <ul>
 *   <li>验证码：key=sms:code:{phone} value=code TTL=5min（一次性，验证通过后删除）</li>
 *   <li>发送冷却：key=sms:lock:{phone} value=1 TTL=60s（setIfAbsent 原子获取，防竞态重复发送）</li>
 * </ul>
 *
 * <p>迁移收益：
 * <ol>
 *   <li>多实例共享：user-service 水平扩容后，任意实例发送/验证都能命中</li>
 *   <li>重启不丢失：服务重启不影响已发送且未过期的验证码</li>
 *   <li>无需手动清理：Redis TTL 自动过期删除，替代原 cleanupExpired</li>
 * </ol>
 */
@Service
public class SmsServiceImpl implements SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final Environment environment;

    /**
     * 是否在接口响应中返回验证码（devCode）。
     * 默认 true 方便本地调试；一旦激活 prod profile 则强制关闭，防止生产泄露 OTP 导致任意账号被接管。
     */
    @Value("${sms.expose-dev-code:true}")
    private boolean exposeDevCodeProp;

    public SmsServiceImpl(StringRedisTemplate redisTemplate, Environment environment) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    /** 仅当未激活 prod profile 且开关为真时才返回验证码 */
    private boolean shouldExposeDevCode() {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        return exposeDevCodeProp && !prod;
    }

    /** Redis key 前缀 */
    private static final String KEY_CODE_PREFIX = "sms:code:";
    private static final String KEY_LOCK_PREFIX = "sms:lock:";

    /** 验证码有效期：5 分钟 */
    private static final long TTL_MILLIS = 5 * 60 * 1000L;
    /** 发送冷却：60 秒 */
    private static final long SEND_INTERVAL_MILLIS = 60 * 1000L;
    private static final int TTL_SECONDS = (int) (TTL_MILLIS / 1000);
    private static final int LOCK_SECONDS = (int) (SEND_INTERVAL_MILLIS / 1000);

    /** 验证结果 */
    public static final int VERIFY_OK = 0;
    public static final int VERIFY_EMPTY = 1;       // 验证码为空 / 不存在 / 已过期（Redis 自动过期）
    public static final int VERIFY_EXPIRED = 2;     // 保留常量兼容调用方；Redis 版不再单独返回（TTL 自动过期）
    public static final int VERIFY_WRONG = 3;       // 错误

    /**
     * 发送验证码
     *
     * <p>流程：
     * <ol>
     *   <li>校验手机号格式（11 位、1[3-9] 开头）</li>
     *   <li>SETNX 抢占 60 秒发送冷却锁（原子操作，防并发重复发送）</li>
     *   <li>生成 4 位数字验证码，写入 Redis（TTL 5 分钟）</li>
     *   <li>模拟发送（开发环境打印日志）</li>
     * </ol>
     * </p>
     *
     * @param phone 手机号
     * @return 结果 Map：ok / message / expiresIn / devCode（仅 dev 环境有）
     */
    @Override
    public Map<String, Object> sendCode(String phone) {
        Map<String, Object> result = new HashMap<>();

        // 1. 手机号格式校验
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            result.put("ok", false);
            result.put("message", "手机号格式不正确");
            return result;
        }

        // 2. 发送频率限制：SETNX 原子获取冷却锁（抢占成功才能发送）
        String lockKey = KEY_LOCK_PREFIX + phone;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            long remainSec = (ttl != null && ttl > 0) ? ttl : LOCK_SECONDS;
            result.put("ok", false);
            result.put("message", "发送过于频繁,请 " + remainSec + " 秒后再试");
            result.put("retryAfterSeconds", remainSec);
            return result;
        }

        // 3. 生成 4 位数字验证码
        String code = String.format("%04d", new Random().nextInt(10000));

        // 4. 存入 Redis（TTL 5 分钟，自动过期，无需手动清理）
        redisTemplate.opsForValue().set(KEY_CODE_PREFIX + phone, code, TTL_SECONDS, TimeUnit.SECONDS);

        // 5. 模拟发送（实际项目接入短信服务商 API）
        logger.info("============================================");
        logger.info("【短信验证码】  收件人:{}  验证码:{}", phone, code);
        logger.info("============================================");

        result.put("ok", true);
        result.put("message", "验证码发送成功");
        result.put("expiresIn", TTL_SECONDS);
        // 仅非生产环境才回显验证码方便调试；生产（prod profile 或 SMS_EXPOSE_DEV_CODE=false）绝不返回
        if (shouldExposeDevCode()) {
            result.put("devCode", code);
        }
        return result;
    }

    /**
     * 校验验证码（一次性使用，验证通过后立即从 Redis 删除）
     *
     * <p>说明：Redis TTL 自动过期，key 不存在时统一返回 {@link #VERIFY_EMPTY}
     * （原进程版的 VERIFY_EXPIRED 不再单独区分，调用方兼容无需改动）。
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return 验证结果码：{@link #VERIFY_OK} / {@link #VERIFY_EMPTY} / {@link #VERIFY_WRONG}
     */
    @Override
    public int verifyCode(String phone, String code) {
        if (phone == null || code == null || code.isEmpty()) {
            return VERIFY_EMPTY;
        }
        String codeKey = KEY_CODE_PREFIX + phone;
        String saved = redisTemplate.opsForValue().get(codeKey);
        if (saved == null) {
            // 不存在或已过期（Redis TTL 自动删除）
            return VERIFY_EMPTY;
        }
        if (!saved.equals(code)) {
            return VERIFY_WRONG;
        }
        // 一次性使用：验证通过后删除
        redisTemplate.delete(codeKey);
        return VERIFY_OK;
    }
}
