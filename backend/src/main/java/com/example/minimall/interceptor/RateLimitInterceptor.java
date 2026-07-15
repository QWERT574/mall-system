package com.example.minimall.interceptor;

import com.example.minimall.annotation.RateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流拦截器。
 * <p>
 * 基于 Redis + {@link RateLimit} 注解实现按 IP+方法名的滑动计数限流：
 * 在时间窗内累计访问次数，超过阈值返回 429；
 * Redis 异常时为保证可用性会放行请求。
 * </p>
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 构造方法，注入字符串类型的 RedisTemplate。
     */
    public RateLimitInterceptor(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 拦截入口：读取方法上的 {@link RateLimit} 注解，结合 IP 与方法名计数；
     * 超过阈值则返回 429，否则自增计数并刷新过期时间。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        try {
            String ip = getClientIp(request);
            String key = "rate_limit:" + ip + ":" + method.getName();
            
            String countStr = redisTemplate.opsForValue().get(key);
            int count = countStr == null ? 0 : Integer.parseInt(countStr);
            
            if (count >= rateLimit.limit()) {
                logger.warn("IP {} 访问 {} 超过频率限制，当前次数：{}", ip, method.getName(), count);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":1,\"message\":\"请求过于频繁，请稍后再试\"}");
                return false;
            }
            
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, rateLimit.timeout(), TimeUnit.SECONDS);
        } catch (Exception e) {
            // Redis 连接失败，为了保证系统可用性，直接放行
            logger.warn("Redis 连接失败，限流功能将不生效：{}", e.getMessage());
            return true;
        }
        
        return true;
    }

    /**
     * 按优先级从常见代理头中解析客户端真实 IP，兜底使用 remoteAddr。
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
