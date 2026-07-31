package com.example.minimall.controller;

import com.example.minimall.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器 — 订单支付（模拟支付）。
 *
 * <p>网关路由 /api/payment/** → payment-service；前端调用 POST /api/payment/pay/{orderId}。
 * 响应结构与原 order-service 的 /api/order/pay/{id} 保持一致（{code,message,data}），确保前端无感迁移。
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 支付订单（模拟支付）。
     *
     * @param orderId     订单 ID
     * @param paymentInfo 支付信息（paymentMethod / transactionId / remark，均可空）
     * @return {code:0,message:"success",data:"支付成功"} 成功；{code:1,message:"支付失败：..."} 失败
     */
    @PostMapping("/pay/{orderId}")
    public Map<String, Object> payOrder(@PathVariable Long orderId,
                                        @RequestBody(required = false) Map<String, Object> paymentInfo) {
        try {
            logger.info("Pay order request: orderId={}, paymentInfo={}", orderId, paymentInfo);
            paymentService.payOrder(orderId, paymentInfo);
            return createSuccessResponse("支付成功");
        } catch (Exception e) {
            logger.error("Pay order failed", e);
            return createErrorResponse("支付失败：" + e.getMessage());
        }
    }

    /** 构建成功响应（与原 order-service OrderController 响应结构一致） */
    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

    /** 构建错误响应 */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 1);
        response.put("message", message);
        return response;
    }
}
