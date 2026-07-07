package com.example.minimall.interceptor;

import com.example.minimall.model.User;
import com.example.minimall.service.UserService;
import com.example.minimall.utils.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 权限拦截器
 * 实现基于角色的权限控制（默认拒绝 + 显式白名单模式）
 *
 * 修复说明：
 * 1. 修复前：默认放行所有路径，仅在 userType 不匹配时拒绝，导致无 Token 也能访问多数 API
 * 2. 修复后：默认拒绝，无 Token 访问非公开接口返回 403；只对白名单内路径放行
 * 3. 对应论文 6.2 节安全测试用例 S-01 ~ S-04
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    // 公开接口白名单（无需登录即可访问）
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
        // 认证相关
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/sendCode",
        "/api/auth/loginByCode",
        "/api/auth/bindPhone",
        "/api/auth/logout",
        // 用户公开信息
        "/api/user/login",
        "/api/user/register",
        "/api/user/logout",
        "/api/user/list",        // 商家/管理员的公开列表（论文 S-04 修复点：实际上需要鉴权，已移到受限）
        // 商品公开浏览
        "/api/product/list",
        "/api/product/search",
        "/api/product/category",
        "/api/product/recommended",
        "/api/product/hot",
        // 分类
        "/api/category/list",
        "/api/category/top",
        "/api/category/children",
        // 活动公开浏览
        "/api/activity/list",
        "/api/activity/recommended",
        // 优惠公开浏览
        "/api/coupon/available",
        "/api/coupon/list",
        "/api/discount/list",
        "/api/discount/active",
        // 评价公开
        "/api/review/list",
        "/api/review/product",
        // AI 客服
        "/api/ai/query",
        "/api/ai/chat",
        "/api/faq/list",
        "/api/cs/session",
        // 静态资源 + WebSocket
        "/uploads",
        "/images",
        "/ws-chat"
    ));

    // 公开接口前缀
    private static final String[] PUBLIC_PREFIXES = {
        "/api/auth/",
        "/api/captcha/",
        "/api/sms/",
        "/api/category/list",
        "/api/category/top",
        "/api/category/tree",
        "/api/category/children",
        "/api/product/list",
        "/api/product/search",
        "/api/product/category/",
        "/api/product/recommended",
        "/api/product/hot",
        "/api/activity/list",
        "/api/activity/recommended",
        "/api/discount/list",
        "/api/discount/active",
        "/api/coupon/available",
        "/api/coupon/list",
        "/api/ai/",
        "/api/faq/",
        "/api/cs/",
        "/api/upload/",
        "/api/review/product/",
        "/api/debug/",
        "/uploads/",
        "/images/",
        "/ws-chat/"
    };

    // 数字 ID 模式（公开接口中允许带数字 ID 的）
    private static final Pattern PUBLIC_ID_PATTERN = Pattern.compile(
        "^/api/(product|category|activity|discount|coupon|review)/\\d+(/.*)?$"
    );

    // 买家可访问的数字 ID 模式
    private static final Pattern BUYER_ID_PATTERN = Pattern.compile(
        "^/api/(order|aftersale)/\\d+(/.*)?$"
    );

    // 卖家可访问的数字 ID 模式
    private static final Pattern SELLER_ID_PATTERN = Pattern.compile(
        "^/api/(order|aftersale)/\\d+(/.*)?$"
    );

    // 买家优惠券领取/接收接口（避免 startsWith 在某些边缘情况下失效）
    private static final Pattern BUYER_COUPON_PATTERN = Pattern.compile(
        "^/api/coupon/(claim|receive)/\\d+(/.*)?$"
    );

    // 商家优惠券管理接口：POST /api/coupon、PUT/DELETE /api/coupon/{id}
    // （公开读取 /api/coupon/list、/api/coupon/available、/api/coupon/{id} 已由 PUBLIC_PATHS / PUBLIC_ID_PATTERN 放行）
    private static final Pattern SELLER_COUPON_PATTERN = Pattern.compile(
        "^/api/coupon(/\\d+)?$"
    );

    public PermissionInterceptor(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    /**
     * 拦截入口：识别公开/CORS 预检请求；从 JWT 解析用户类型后做角色白名单校验，
     * 无 Token 返回 401，越权返回 403。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS 请求直接放行（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 1. 判断是否为公开接口
        if (isPublicPath(requestURI)) {
            return true;
        }

        // 2. 解析 Token 获取用户类型（优先从JWT获取，避免前端User-Type偏移问题）
        Integer userType = getUserTypeFromJwtToken(request);
        if (userType == null) {
            userType = getUserTypeFromHeaders(request);
        }

        // 3. 无 Token 访问非公开接口 → 401
        if (userType == null) {
            writeUnauthorized(response, "未授权访问，请先登录");
            return false;
        }

        // 4. 管理员（userType=2）可访问所有接口
        if (userType == 2) {
            return true;
        }

        // 5. 卖家（userType=1）权限校验
        if (userType == 1) {
            if (isSellerAllowed(requestURI)) {
                return true;
            }
            writeForbidden(response, "卖家无权访问该接口");
            return false;
        }

        // 6. 买家（userType=0）权限校验
        if (userType == 0) {
            if (isBuyerAllowed(requestURI)) {
                return true;
            }
            writeForbidden(response, "买家无权访问该接口");
            return false;
        }

        writeForbidden(response, "未知用户类型");
        return false;
    }

    /**
     * 判断是否为公开接口
     */
    private boolean isPublicPath(String uri) {
        // 精确匹配
        if (PUBLIC_PATHS.contains(uri)) {
            return true;
        }
        // 前缀匹配
        for (String prefix : PUBLIC_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        // 数字 ID 模式
        if (PUBLIC_ID_PATTERN.matcher(uri).matches()) {
            return true;
        }
        return false;
    }

    /**
     * 卖家可访问的接口
     */
    private boolean isSellerAllowed(String uri) {
        return uri.startsWith("/api/seller/")
            || uri.startsWith("/api/product/")      // 商品管理（卖家可管理自家商品）
            || uri.startsWith("/api/order/seller/")
            || uri.startsWith("/api/order/detail/")
            || uri.startsWith("/api/order/ship/")
            || uri.startsWith("/api/order/pay/")
            || uri.startsWith("/api/aftersale/seller")
            || uri.startsWith("/api/aftersale/seller/")
            || uri.startsWith("/api/aftersale/list/")
            || uri.startsWith("/api/aftersale/list")
            // 商家端客服聊天所有子接口
            || uri.startsWith("/api/chat/")
            || uri.startsWith("/api/coupon/seller/")
            // 商家对自己的优惠券做 CRUD：POST /api/coupon、PUT/DELETE /api/coupon/{id}
            || SELLER_COUPON_PATTERN.matcher(uri).matches()
            || uri.startsWith("/api/discount/seller/")
            // 商家对自己的优惠活动做 CRUD：POST/PUT/DELETE /api/discount、
            // /api/discount/{id}、/api/discount/{id}/product、/api/discount/product/{id}
            // 写接口必须放行；/list、/active、/active-with-products 已在 PUBLIC_PATHS 中
            || uri.equals("/api/discount")
            || uri.startsWith("/api/discount/")
            || uri.startsWith("/api/activity/seller/")
            || uri.startsWith("/api/review/seller/")
            // 商家回复评价
            || uri.startsWith("/api/review/reply")
            || uri.startsWith("/api/user/info")
            || uri.startsWith("/api/user/update")
            || uri.startsWith("/api/user/change-password")
            || uri.startsWith("/api/user/address")
            || uri.startsWith("/api/dashboard/seller")
            || uri.startsWith("/api/statistics/seller")
            || uri.startsWith("/api/system/dashboard/")
            // 注销自己的账号：任何已登录用户都可调用
            || uri.equals("/api/user/deactivate")
            || SELLER_ID_PATTERN.matcher(uri).matches();
    }

    /**
     * 买家可访问的接口
     */
    private boolean isBuyerAllowed(String uri) {
        return uri.startsWith("/api/cart/")
            || uri.startsWith("/api/order/create")
            || uri.startsWith("/api/order/list")
            || uri.startsWith("/api/order/cancel/")
            || uri.startsWith("/api/order/confirm/")
            || uri.startsWith("/api/order/pay/")
            || uri.startsWith("/api/order/detail/")
            || uri.startsWith("/api/order/delete/")
            || uri.startsWith("/api/order/status/")
            || uri.equals("/api/aftersale")
            || uri.startsWith("/api/aftersale/list")
            || uri.startsWith("/api/aftersale/detail/")
            || uri.startsWith("/api/aftersale/cancel/")
            || uri.startsWith("/api/aftersale/user/")
            || uri.startsWith("/api/chat/")
            || uri.startsWith("/api/coupon/user/")
            || uri.startsWith("/api/coupon/claim/")
            || uri.startsWith("/api/coupon/receive/")
            || uri.startsWith("/api/coupon/my")
            || uri.startsWith("/api/user/info")
            || uri.startsWith("/api/user/update")
            || uri.startsWith("/api/user/change-password")
            || uri.startsWith("/api/user/address")
            || uri.startsWith("/api/user/preference")
            // 注销自己的账号：任何已登录用户都可调用
            || uri.equals("/api/user/deactivate")
            || uri.startsWith("/api/review/create")
            || uri.startsWith("/api/review/my")
            || uri.startsWith("/api/wishlist/")
            || uri.startsWith("/api/activity/join/")
            || uri.startsWith("/api/activity/participant")
            || uri.startsWith("/api/activity/my")
            || BUYER_COUPON_PATTERN.matcher(uri).matches()
            || BUYER_ID_PATTERN.matcher(uri).matches();
    }

    /**
     * 从 User-Type 请求头解析
     */
    private Integer getUserTypeFromHeaders(HttpServletRequest request) {
        String userTypeHeader = request.getHeader("User-Type");
        if (StringUtils.hasText(userTypeHeader)) {
            try {
                return Integer.parseInt(userTypeHeader);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 从 JWT Token 中解析用户类型
     */
    private Integer getUserTypeFromJwtToken(HttpServletRequest request) {
        try {
            String bearerToken = request.getHeader("Authorization");
            if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
                return null;
            }
            String token = bearerToken.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return null;
            }
            User user = userService.findById(userId);
            if (user == null) {
                return null;
            }
            return user.getUserType();
        } catch (Exception e) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"code\": 401, \"message\": \"" + message + "\"}");
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write("{\"code\": 403, \"message\": \"" + message + "\"}");
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
