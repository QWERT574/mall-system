package com.example.minimall.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

/**
 * API 访问日志拦截器。
 * <p>
 * 在请求开始时生成 requestId、记录请求方法/URI/参数/IP/User-Agent 等信息，
 * 在请求结束时输出响应状态与处理耗时，便于链路追踪与问题排查。
 * </p>
 */
@Component
public class ApiLoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiLoggingInterceptor.class);

    /**
     * 拦截入口：生成请求 ID，记录请求信息并保存开始时间，返回 true 放行。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestId = java.util.UUID.randomUUID().toString();
        request.setAttribute("requestId", requestId);
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[请求ID: ").append(requestId).append("] ");
        logMessage.append("请求方法: ").append(request.getMethod()).append(" | ");
        logMessage.append("请求URI: ").append(request.getRequestURI()).append(" | ");
        logMessage.append("查询参数: ").append(getQueryString(request)).append(" | ");
        logMessage.append("客户端IP: ").append(getClientIp(request)).append(" | ");
        logMessage.append("User-Agent: ").append(request.getHeader("User-Agent"));
        
        logger.info(logMessage.toString());
        
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        
        return true;
    }

    /**
     * 请求完成钩子：记录响应状态、耗时，异常时记录错误信息。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestId = (String) request.getAttribute("requestId");
        Long startTime = (Long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[请求ID: ").append(requestId).append("] ");
        logMessage.append("响应状态: ").append(response.getStatus()).append(" | ");
        logMessage.append("处理耗时: ").append(duration).append("ms");
        
        if (ex != null) {
            logMessage.append(" | 异常信息: ").append(ex.getMessage());
            logger.error(logMessage.toString(), ex);
        } else {
            logger.info(logMessage.toString());
        }
    }

    /**
     * 将请求参数序列化为 key=value&amp;key=value 字符串，便于日志输出。
     */
    private String getQueryString(HttpServletRequest request) {
        Enumeration<String> paramNames = request.getParameterNames();
        StringBuilder sb = new StringBuilder();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            sb.append(paramName).append("=").append(paramValue);
            if (paramNames.hasMoreElements()) {
                sb.append("&");
            }
        }
        return sb.toString();
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
