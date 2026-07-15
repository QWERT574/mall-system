package com.example.minimall.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 接口性能监控拦截器。
 * <p>
 * 记录接口处理耗时：超过 1 秒的视为慢请求并 WARN 告警，
 * 其余在 DEBUG 级别输出，便于定位性能瓶颈。
 * </p>
 */
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceInterceptor.class);
    private static final String PERF_START_TIME_ATTR = "perfStartTime";
    private static final long SLOW_THRESHOLD_MS = 1000;

    /**
     * 拦截入口：记录请求开始时间戳。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(PERF_START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    /**
     * Controller 执行完成后统计耗时，区分慢请求与正常请求输出日志。
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        Long startTime = (Long) request.getAttribute(PERF_START_TIME_ATTR);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String uri = request.getRequestURI();
            String method = request.getMethod();
            
            if (duration > SLOW_THRESHOLD_MS) {
                logger.warn("慢请求警告 - {} {} - 耗时：{}ms", method, uri, duration);
            } else {
                logger.debug("API 请求 - {} {} - 耗时：{}ms", method, uri, duration);
            }
        }
    }
    
    /**
     * 请求完成时清理请求属性中的开始时间戳。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        request.removeAttribute(PERF_START_TIME_ATTR);
    }
}
