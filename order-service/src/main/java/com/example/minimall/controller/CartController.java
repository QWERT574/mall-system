package com.example.minimall.controller;

import com.example.minimall.model.Cart;
import com.example.minimall.service.CartService;
import com.example.minimall.utils.JwtUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 购物车相关接口 */
@RestController
@RequestMapping("/api/cart")
public class CartController {
    /** 购物车业务服务 */
    private final CartService cartService;
    /** JWT 工具，用于从 token 解析 userId */
    private final JwtUtil jwtUtil;

    public CartController(CartService cartService, JwtUtil jwtUtil) {
        this.cartService = cartService;
        this.jwtUtil = jwtUtil;
    }

    /** 获取当前用户的购物车列表（userId 可通过参数或 token 传入） */
    @GetMapping("/list")
    public Map<String, Object> getCartList(@RequestParam(required = false) Long userId, HttpServletRequest request) {
        try {
            if (userId == null) {
                userId = getUserIdFromToken(request);
            }
            if (userId == null) {
                return createErrorResponse("缺少用户ID");
            }
            List<Map<String, Object>> cartList = cartService.findByUserId(userId);
            return createSuccessResponse(cartList);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取购物车列表失败: " + e.getMessage());
        }
    }

    /** 添加商品到购物车 */
    @PostMapping("/add")
    public Map<String, Object> addToCart(@RequestBody Cart cart) {
        try {
            cartService.save(cart);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("添加到购物车失败: " + e.getMessage());
        }
    }

    /** 更新购物车单项的数量 */
    @PostMapping("/{id}/update-quantity")
    public Map<String, Object> updateQuantity(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            cartService.updateQuantity(id, body.get("quantity"));
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新购物车数量失败: " + e.getMessage());
        }
    }

    /** 更新购物车单项的勾选状态 */
    @PostMapping("/{id}/update-checked")
    public Map<String, Object> updateChecked(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            cartService.updateChecked(id, body.get("checked"));
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新购物车选中状态失败: " + e.getMessage());
        }
    }

    /** 删除购物车中的某一项 */
    @PostMapping("/{id}/delete")
    public Map<String, Object> deleteCartItem(@PathVariable Long id) {
        try {
            cartService.delete(id);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("删除购物车项失败: " + e.getMessage());
        }
    }

    /** 清空指定用户的购物车 */
    @PostMapping("/clear")
    public Map<String, Object> clearCart(@RequestBody Map<String, Long> body) {
        try {
            cartService.clearByUserId(body.get("userId"));
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("清空购物车失败: " + e.getMessage());
        }
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.getUserIdFromToken(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
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
