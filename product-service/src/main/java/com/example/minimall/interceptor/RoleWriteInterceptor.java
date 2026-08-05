package com.example.minimall.interceptor;

import com.example.minimall.feign.UserFeignClient;
import com.example.minimall.model.User;
import com.example.minimall.utils.JwtUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 商品域写操作角色拦截器（安全修复）。
 *
 * <p>背景：product-service 此前只有 Spring Security 的 {@code .authenticated()}，没有任何角色校验，
 * 导致任意已登录买家即可创建/修改/删除商品、优惠券、折扣、活动等卖家/管理员资源（越权）。
 *
 * <p>设计（方法感知，最小回归）：
 * <ul>
 *   <li>读操作（GET/HEAD）默认放行——是否需要登录由 SecurityConfig 决定；仅管理台仪表盘
 *       {@code /api/system/dashboard/**} 需要卖家或管理员。</li>
 *   <li>写操作（POST/PUT/DELETE/PATCH）：买家合法写（评价、领券、清理搜索历史）放行；
 *       {@code /api/system/**} 写要求管理员；其余写要求卖家或管理员。</li>
 *   <li>内部 Feign 调用 {@code /api/internal/**} 放行（由 InternalTokenFilter 校验令牌）。</li>
 * </ul>
 * userType 来源于校验后的 JWT + 经 Feign 调 user-service 查询（0=买家/1=卖家/2=管理员）。
 */
@Component
public class RoleWriteInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserFeignClient userFeignClient;

    public RoleWriteInterceptor(JwtUtil jwtUtil, @Lazy UserFeignClient userFeignClient) {
        this.jwtUtil = jwtUtil;
        this.userFeignClient = userFeignClient;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // CORS 预检放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        // 内部服务间调用（由 InternalTokenFilter 校验 X-Internal-Token）
        if (uri.startsWith("/api/internal/")) {
            return true;
        }

        boolean write = "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);

        if (!write) {
            // 读操作：仅管理台仪表盘限制卖家/管理员，其余读放行
            if (uri.startsWith("/api/system/dashboard")) {
                return requireRole(request, response, false);
            }
            return true;
        }

        // 写操作
        if (uri.startsWith("/api/system")) {
            // 系统配置写入仅管理员
            return requireRole(request, response, true);
        }
        if (isBuyerWrite(uri)) {
            // 买家合法写操作（已由 SecurityConfig 保证已登录）
            return true;
        }
        // 其余写（商品/优惠券/折扣/活动/评价回复等）要求卖家或管理员
        return requireRole(request, response, false);
    }

    /** 判断是否为买家允许的写操作 */
    private boolean isBuyerWrite(String uri) {
        // 商家回复评价属于卖家操作，明确排除
        if (uri.startsWith("/api/review/reply")) {
            return false;
        }
        return uri.startsWith("/api/coupon/claim/")          // 买家领取优惠券
            || uri.equals("/api/review")                     // 买家创建评价
            || uri.startsWith("/api/review/submit")          // 买家批量提交评价
            || uri.matches("^/api/review/\\d+$")             // 买家修改/删除自己的评价
            || uri.startsWith("/api/product/search-history"); // 买家清理自己的搜索历史
    }

    /**
     * 要求卖家或管理员（adminOnly=true 时仅管理员）。
     */
    private boolean requireRole(HttpServletRequest request, HttpServletResponse response, boolean adminOnly) throws Exception {
        Integer userType = resolveUserType(request);
        if (userType == null) {
            write(response, HttpServletResponse.SC_UNAUTHORIZED, "未授权访问，请先登录");
            return false;
        }
        boolean allowed = adminOnly ? (userType == 2) : (userType == 1 || userType == 2);
        if (allowed) {
            return true;
        }
        write(response, HttpServletResponse.SC_FORBIDDEN, "无权访问该接口");
        return false;
    }

    /** 从校验后的 JWT 解析 userId，再经 Feign 查询 user-service 得到 userType */
    private Integer resolveUserType(HttpServletRequest request) {
        try {
            String bearer = request.getHeader("Authorization");
            if (!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
                return null;
            }
            String token = bearer.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return null;
            }
            User user = userFeignClient.getUser(userId);
            return user == null ? null : user.getUserType();
        } catch (Exception e) {
            return null;
        }
    }

    private void write(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\": " + status + ", \"message\": \"" + message + "\"}");
    }
}
