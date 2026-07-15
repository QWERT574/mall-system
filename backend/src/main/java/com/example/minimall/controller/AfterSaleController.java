package com.example.minimall.controller;

import com.example.minimall.model.AfterSaleService;
import com.example.minimall.service.AfterSaleServiceApi;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 售后服务申请（退款/退货/仲裁）相关接口 */
@RestController
@RequestMapping("/api/aftersale")
public class AfterSaleController {
    /** 售后业务服务 */
    private final AfterSaleServiceApi afterSaleServiceApi;

    public AfterSaleController(AfterSaleServiceApi afterSaleServiceApi) {
        this.afterSaleServiceApi = afterSaleServiceApi;
    }

    /** 创建售后服务申请 */
    @PostMapping
    public Map<String, Object> createAfterSale(@Valid @RequestBody AfterSaleService afterSale) {
        try {
            AfterSaleService created = afterSaleServiceApi.createAfterSale(afterSale);
            return createSuccessResponse(created);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("创建售后服务申请失败: " + e.getMessage());
        }
    }

    /** 根据 ID 获取售后服务申请详情 */
    @GetMapping("/{id}")
    public Map<String, Object> getAfterSaleById(@PathVariable Long id) {
        try {
            AfterSaleService afterSale = afterSaleServiceApi.getAfterSaleById(id);
            if (afterSale != null) {
                return createSuccessResponse(afterSale);
            } else {
                return createErrorResponse("售后服务申请不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取售后服务申请失败: " + e.getMessage());
        }
    }

    /** 获取指定用户的所有售后服务申请 */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getAfterSalesByUserId(@PathVariable Long userId) {
        try {
            List<AfterSaleService> afterSales = afterSaleServiceApi.getAfterSalesByUserId(userId);
            return createSuccessResponse(afterSales);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取用户售后服务列表失败: " + e.getMessage());
        }
    }

    /** 分页获取指定用户售后服务列表，可按状态筛选 */
    @GetMapping("/user/{userId}/page")
    public Map<String, Object> getAfterSalesByUserIdWithPage(@PathVariable Long userId,
                                                             @RequestParam(defaultValue = "1") Integer page,
                                                             @RequestParam(defaultValue = "10") Integer size,
                                                             @RequestParam(required = false) Integer status) {
        System.out.println("===== [Controller] 收到请求: userId=" + userId + ", page=" + page + ", size=" + size + ", status=" + status + " =====");
        try {
            com.baomidou.mybatisplus.core.metadata.IPage<AfterSaleService> result = afterSaleServiceApi.getAfterSalesByUserIdWithPage(userId, page, size, status);
            System.out.println("===== [Controller] Service返回: total=" + result.getTotal() + ", records=" + (result.getRecords() != null ? result.getRecords().size() : 0) + " =====");
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("total", result.getTotal());
            pageInfo.put("records", result.getRecords());
            pageInfo.put("current", result.getCurrent());
            pageInfo.put("size", result.getSize());
            pageInfo.put("pages", result.getPages());
            return createSuccessResponse(pageInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取用户售后服务列表失败: " + e.getMessage());
        }
    }

    /** 根据订单 ID 获取售后服务申请列表 */
    @GetMapping("/order/{orderId}")
    public Map<String, Object> getAfterSalesByOrderId(@PathVariable Long orderId) {
        try {
            List<AfterSaleService> afterSales = afterSaleServiceApi.getAfterSalesByOrderId(orderId);
            return createSuccessResponse(afterSales);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取订单售后服务列表失败: " + e.getMessage());
        }
    }

    /** 分页获取全部售后申请，可按状态筛选（管理端） */
    @GetMapping("/list")
    public Map<String, Object> getAfterSalesPage(@RequestParam(defaultValue = "1") Integer page, 
                                                @RequestParam(defaultValue = "10") Integer size, 
                                                @RequestParam(required = false) Integer status) {
        System.out.println("===== list接口被调用! page=" + page + " =====");
        try {
            com.baomidou.mybatisplus.core.metadata.IPage<AfterSaleService> result = afterSaleServiceApi.getAfterSalesPage(page, size, status);

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("total", result.getTotal());
            pageInfo.put("records", result.getRecords());
            pageInfo.put("current", result.getCurrent());
            pageInfo.put("size", result.getSize());
            pageInfo.put("pages", result.getPages());

            return createSuccessResponse(pageInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取售后服务列表失败: " + e.getMessage());
        }
    }

    /** 商家分页查询自己名下的售后申请 */
    @GetMapping("/seller")
    public Map<String, Object> getAfterSalesBySellerId(@RequestParam Long sellerId,
                                                        @RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) Integer status) {
        try {
            System.out.println("===== Controller: 获取商家售后列表 - sellerId=" + sellerId + " =====");

            com.baomidou.mybatisplus.core.metadata.IPage<AfterSaleService> result = afterSaleServiceApi.getAfterSalesBySellerIdWithPage(sellerId, page, size, status);
            System.out.println("===== Controller: 查询结果 - total=" + result.getTotal() + ", records=" + result.getRecords().size() + " =====");

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("total", result.getTotal());
            pageInfo.put("records", result.getRecords());
            pageInfo.put("current", result.getCurrent());
            pageInfo.put("size", result.getSize());
            pageInfo.put("pages", result.getPages());

            return createSuccessResponse(pageInfo);
        } catch (Exception e) {
            System.err.println("===== Controller: 异常 - " + e.getMessage() + " =====");
            e.printStackTrace();
            return createErrorResponse("获取商家售后服务列表失败: " + e.getMessage());
        }
    }

    /** 更新售后申请信息 */
    @PutMapping("/{id}")
    public Map<String, Object> updateAfterSale(@PathVariable Long id, @Valid @RequestBody AfterSaleService afterSale) {
        try {
            AfterSaleService updated = afterSaleServiceApi.updateAfterSale(id, afterSale);
            return createSuccessResponse(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新售后服务申请失败: " + e.getMessage());
        }
    }

    /** 处理售后申请（POST 方式）：状态/结果/退款 */
    @PostMapping("/{id}/process")
    public Map<String, Object> processAfterSale(@PathVariable Long id, 
                                               @RequestBody Map<String, Object> request) {
        try {
            Integer status = (Integer) request.get("status");
            String serviceResult = (String) request.get("serviceResult");
            Long operatorId = request.get("operatorId") != null ? Long.valueOf(request.get("operatorId").toString()) : null;
            BigDecimal refundAmount = request.get("refundAmount") != null ? new BigDecimal(request.get("refundAmount").toString()) : null;
            
            AfterSaleService processed = afterSaleServiceApi.processAfterSale(id, status, serviceResult, operatorId, refundAmount);
            return createSuccessResponse(processed);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("处理售后服务申请失败: " + e.getMessage());
        }
    }

    /** 处理售后申请（PUT 方式，参数以 query string 传入） */
    @PutMapping("/{id}/process")
    public Map<String, Object> processAfterSalePut(@PathVariable Long id,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) String serviceResult,
                                                   @RequestParam(required = false) Long operatorId,
                                                   @RequestParam(required = false) BigDecimal refundAmount) {
        try {
            AfterSaleService processed = afterSaleServiceApi.processAfterSale(id, status, serviceResult, operatorId, refundAmount);
            return createSuccessResponse(processed);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("处理售后服务申请失败: " + e.getMessage());
        }
    }

    /** 补充售后举证材料 */
    @PostMapping("/{id}/evidence")
    public Map<String, Object> addSupplementaryEvidence(@PathVariable Long id,
                                                        @RequestBody Map<String, Object> request) {
        try {
            String evidence = (String) request.get("evidence");
            Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
            AfterSaleService result = afterSaleServiceApi.addSupplementaryEvidence(id, evidence, userId);
            return createSuccessResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("补充证据失败: " + e.getMessage());
        }
    }

    /** 取消售后申请 */
    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancelAfterSale(@PathVariable Long id,
                                               @RequestBody Map<String, Object> request) {
        try {
            Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
            AfterSaleService result = afterSaleServiceApi.cancelAfterSale(id, userId);
            return createSuccessResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("取消售后申请失败: " + e.getMessage());
        }
    }

    /** 更新退货物流信息 */
    @PutMapping("/{id}/logistics")
    public Map<String, Object> updateReturnLogistics(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> request) {
        try {
            String logisticsCompany = (String) request.get("logisticsCompany");
            String logisticsNo = (String) request.get("logisticsNo");
            Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
            AfterSaleService result = afterSaleServiceApi.updateReturnLogistics(id, logisticsCompany, logisticsNo, userId);
            return createSuccessResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("更新退货物流失败: " + e.getMessage());
        }
    }

    /** 获取售后申请的处理记录 */
    @GetMapping("/{id}/records")
    public Map<String, Object> getServiceRecords(@PathVariable Long id) {
        try {
            List<com.example.minimall.model.ServiceRecord> records = afterSaleServiceApi.getServiceRecords(id);
            return createSuccessResponse(records);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("获取服务记录失败: " + e.getMessage());
        }
    }

    /** 删除售后申请 */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteAfterSale(@PathVariable Long id) {
        try {
            afterSaleServiceApi.deleteAfterSale(id);
            return createSuccessResponse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("删除售后服务申请失败: " + e.getMessage());
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