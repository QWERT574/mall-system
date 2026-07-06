package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.minimall.model.Coupon;
import com.example.minimall.model.UserCoupon;
import com.example.minimall.service.CouponService;
import com.example.minimall.utils.JwtUtil;
import com.example.minimall.service.UserService;
import com.example.minimall.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 优惠券（创建/领取/使用/计算）相关接口 */
@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    /** 优惠券业务服务 */
    @Autowired
    private CouponService couponService;

    /** JWT 工具，用于从 token 解析 userId */
    @Autowired
    private JwtUtil jwtUtil;

    /** 用户业务服务 */
    @Autowired
    private UserService userService;

    /** 分页获取可用优惠券 */
    @GetMapping("/available")
    public Map<String, Object> getAvailableCoupons(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            IPage<Coupon> result = couponService.getAvailableCoupons(page, size);
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("total", result.getTotal());
            pageInfo.put("records", result.getRecords());
            pageInfo.put("current", result.getCurrent());
            pageInfo.put("size", result.getSize());
            pageInfo.put("pages", result.getPages());
            return createSuccessResponse(pageInfo);
        } catch (Exception e) {
            return createErrorResponse("获取可用优惠券失败: " + e.getMessage());
        }
    }

    /** 根据 ID 获取优惠券详情 */
    @GetMapping("/{id}")
    public Map<String, Object> getCoupon(@PathVariable Long id) {
        try {
            Coupon coupon = couponService.findById(id);
            if (coupon == null) return createErrorResponse("优惠券不存在");
            return createSuccessResponse(coupon);
        } catch (Exception e) {
            return createErrorResponse("获取优惠券失败: " + e.getMessage());
        }
    }

    /** 商家分页查询自己创建的优惠券 */
    @GetMapping("/list")
    public Map<String, Object> getSellerCoupons(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        try {
            IPage<Coupon> result = couponService.getSellerCoupons(sellerId, page, size, status);
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("total", result.getTotal());
            pageInfo.put("records", result.getRecords());
            pageInfo.put("current", result.getCurrent());
            pageInfo.put("size", result.getSize());
            pageInfo.put("pages", result.getPages());
            return createSuccessResponse(pageInfo);
        } catch (Exception e) {
            return createErrorResponse("获取优惠券列表失败: " + e.getMessage());
        }
    }

    /** 创建优惠券 */
    @PostMapping
    public Map<String, Object> createCoupon(@RequestBody Coupon coupon) {
        try {
            Coupon created = couponService.createCoupon(coupon);
            return createSuccessResponse(created);
        } catch (Exception e) {
            return createErrorResponse("创建优惠券失败: " + e.getMessage());
        }
    }

    /** 更新优惠券 */
    @PutMapping("/{id}")
    public Map<String, Object> updateCoupon(@PathVariable Long id, @RequestBody Coupon coupon) {
        try {
            Coupon updated = couponService.updateCoupon(id, coupon);
            return createSuccessResponse(updated);
        } catch (Exception e) {
            return createErrorResponse("更新优惠券失败: " + e.getMessage());
        }
    }

    /** 删除优惠券 */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCoupon(@PathVariable Long id) {
        try {
            couponService.deleteCoupon(id);
            return createSuccessResponse("删除成功");
        } catch (Exception e) {
            return createErrorResponse("删除优惠券失败: " + e.getMessage());
        }
    }

    /** 用户领取优惠券（userId 可从 query/body/token 中获取） */
    @PostMapping("/claim/{couponId}")
    public Map<String, Object> claimCoupon(@PathVariable Long couponId,
                                           @RequestParam(required = false) Long userId,
                                           @RequestBody(required = false) Map<String, Object> params,
                                           HttpServletRequest request) {
        try {
            // 1. 从查询参数获取userId
            Long effectiveUserId = userId;
            // 2. 从请求体获取userId
            if (effectiveUserId == null && params != null && params.get("userId") != null) {
                effectiveUserId = Long.valueOf(params.get("userId").toString());
            }
            // 3. 从JWT Token获取userId
            if (effectiveUserId == null) {
                effectiveUserId = getUserIdFromToken(request);
            }
            if (effectiveUserId == null) {
                return createErrorResponse("缺少用户ID，请先登录");
            }
            UserCoupon userCoupon = couponService.claimCoupon(couponId, effectiveUserId);
            return createSuccessResponse(userCoupon);
        } catch (Exception e) {
            return createErrorResponse("领取优惠券失败: " + e.getMessage());
        }
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        try {
            String bearerToken = request.getHeader("Authorization");
            if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
                return null;
            }
            String token = bearerToken.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            Long uid = jwtUtil.getUserIdFromToken(token);
            return uid;
        } catch (Exception e) {
            return null;
        }
    }

    /** 获取指定用户领取的所有优惠券 */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserCoupons(@PathVariable Long userId) {
        try {
            List<UserCoupon> coupons = couponService.getUserCoupons(userId);
            return createSuccessResponse(coupons);
        } catch (Exception e) {
            return createErrorResponse("获取用户优惠券失败: " + e.getMessage());
        }
    }

    /** 根据 user_coupon ID 获取用户优惠券详情 */
    @GetMapping("/user-coupon/{id}")
    public Map<String, Object> getUserCoupon(@PathVariable Long id) {
        try {
            UserCoupon uc = couponService.getUserCouponById(id);
            if (uc == null) return createErrorResponse("优惠券不存在");
            return createSuccessResponse(uc);
        } catch (Exception e) {
            return createErrorResponse("获取优惠券失败: " + e.getMessage());
        }
    }

    /** 计算指定用户优惠券对应订单金额的优惠额与最终金额 */
    @GetMapping("/calculate/{userCouponId}")
    public Map<String, Object> calculateDiscount(@PathVariable Long userCouponId, @RequestParam BigDecimal amount) {
        try {
            BigDecimal discount = couponService.calculateDiscount(userCouponId, amount);
            Map<String, Object> data = new HashMap<>();
            data.put("discount", discount);
            data.put("finalAmount", amount.subtract(discount));
            return createSuccessResponse(data);
        } catch (Exception e) {
            return createErrorResponse("计算失败: " + e.getMessage());
        }
    }

    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 1);
        response.put("message", message);
        return response;
    }
}
