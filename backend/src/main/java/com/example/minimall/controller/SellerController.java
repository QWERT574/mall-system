package com.example.minimall.controller;

import com.example.minimall.common.Result;
import com.example.minimall.model.User;
import com.example.minimall.service.UserService;
import com.example.minimall.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家管理接口
 * 提供商家资料查询与更新、审核流程、状态启停、统计等管理功能
 */
@RestController
@RequestMapping("/api/seller")
public class SellerController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // 获取商家信息（公开接口，用于商品详情页）
    @GetMapping("/public/{sellerId}")
    public Result<Map<String, Object>> getSellerInfoPublic(@PathVariable Long sellerId) {
        try {
            User seller = userService.findById(sellerId);
            
            if (seller == null) {
                return error("商家不存在");
            }
            
            Map<String, Object> sellerInfo = new HashMap<>();
            sellerInfo.put("id", seller.getId());
            sellerInfo.put("name", seller.getCompanyName() != null ? seller.getCompanyName() : seller.getNickname());
            sellerInfo.put("contact", seller.getUsername());
            sellerInfo.put("phone", seller.getPhone());
            sellerInfo.put("address", seller.getCompanyAddress());
            sellerInfo.put("isVerified", seller.getIsVerified());
            
            return success(sellerInfo);
        } catch (Exception e) {
            return error("获取商家信息失败：" + e.getMessage());
        }
    }

    // 获取当前登录商家信息
    @GetMapping("/info")
    public Result<Map<String, Object>> getSellerInfo(@RequestParam(required = false) Long sellerId, HttpServletRequest request) {
        try {
            if (sellerId == null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    sellerId = jwtUtil.getUserIdFromToken(token);
                }
            }
            if (sellerId == null) {
                return error("缺少商家ID");
            }
            User seller = userService.findById(sellerId);
            
            if (seller == null) {
                return error("商家不存在");
            }
            
            // 验证是否为商家用户
            if (seller.getUserType() != 1) {
                return error("该用户不是商家");
            }
            
            Map<String, Object> sellerInfo = new HashMap<>();
            sellerInfo.put("id", seller.getId());
            sellerInfo.put("companyName", seller.getCompanyName());
            sellerInfo.put("contactName", seller.getUsername());
            sellerInfo.put("phone", seller.getPhone());
            sellerInfo.put("email", seller.getEmail());
            sellerInfo.put("companyAddress", seller.getCompanyAddress());
            sellerInfo.put("isVerified", seller.getIsVerified());
            sellerInfo.put("status", seller.getStatus());
            sellerInfo.put("verificationInfo", seller.getVerificationInfo());
            sellerInfo.put("nickname", seller.getNickname());
            
            return success(sellerInfo);
        } catch (Exception e) {
            return error("获取商家信息失败：" + e.getMessage());
        }
    }

    // 更新商家信息
    @PostMapping("/update")
    public Result<Map<String, Object>> updateSellerInfo(@RequestBody Map<String, Object> request) {
        try {
            Long sellerId = Long.valueOf(request.get("id").toString());
            String name = (String) request.get("name");
            String contact = (String) request.get("contact");
            String phone = (String) request.get("phone");
            String email = (String) request.get("email");
            String address = (String) request.get("address");
            
            User seller = userService.findById(sellerId);
            if (seller == null) {
                return error("商家不存在");
            }
            
            // 更新商家信息
            seller.setCompanyName(name);
            seller.setUsername(contact);
            seller.setPhone(phone);
            seller.setEmail(email);
            seller.setCompanyAddress(address);
            seller.setUpdatedAt(LocalDateTime.now());
            
            userService.save(seller);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "商家信息更新成功");
            return success(result);
        } catch (Exception e) {
            return error("更新商家信息失败：" + e.getMessage());
        }
    }

    // 获取商家状态
    @GetMapping("/status")
    public Result<Map<String, Object>> getSellerStatus(@RequestParam Long sellerId) {
        try {
            User seller = userService.findById(sellerId);
            
            if (seller == null) {
                return error("商家不存在");
            }
            
            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("isVerified", seller.getIsVerified());
            statusInfo.put("status", seller.getStatus());
            statusInfo.put("verificationInfo", seller.getVerificationInfo());
            
            return success(statusInfo);
        } catch (Exception e) {
            return error("获取商家状态失败：" + e.getMessage());
        }
    }
    
    // 获取所有商家列表（管理员使用）
    @GetMapping("/list")
    public Result<Map<String, Object>> getSellerList(
            @RequestParam(required = false) Integer isVerified,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        try {
            // 使用分页查询
            com.baomidou.mybatisplus.core.metadata.IPage<User> pageResult = userService.findSellersWithPagination(
                pageNum, pageSize, keyword, isVerified, status);
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", pageResult.getRecords());
            result.put("total", pageResult.getTotal());
            result.put("pages", pageResult.getPages());
            result.put("current", pageResult.getCurrent());
            
            return success(result);
        } catch (Exception e) {
            return error("获取商家列表失败：" + e.getMessage());
        }
    }
    
    // 获取商家统计信息
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getSellerStatistics() {
        try {
            List<User> sellers = userService.findAllSellers();
            
            long total = sellers.size();
            long pending = sellers.stream().filter(s -> s.getIsVerified() != null && s.getIsVerified() == 0).count();
            long approved = sellers.stream().filter(s -> s.getIsVerified() != null && s.getIsVerified() == 1).count();
            long rejected = sellers.stream().filter(s -> s.getIsVerified() != null && s.getIsVerified() == 2).count();
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("pending", pending);
            result.put("approved", approved);
            result.put("rejected", rejected);
            
            return success(result);
        } catch (Exception e) {
            return error("获取商家统计失败：" + e.getMessage());
        }
    }
    
    // 审核通过商家
    @PostMapping("/approve/{sellerId}")
    public Result<Map<String, Object>> approveSeller(@PathVariable Long sellerId) {
        try {
            User seller = userService.findById(sellerId);
            
            if (seller == null) {
                return error("商家不存在");
            }
            
            if (seller.getUserType() != 1) {
                return error("该用户不是商家");
            }
            
            // 更新审核状态为已通过
            seller.setIsVerified(1);
            seller.setVerificationInfo("审核通过，祝您生意兴隆！");
            seller.setStatus(1); // 启用账号
            seller.setVerifiedAt(LocalDateTime.now());
            seller.setUpdatedAt(LocalDateTime.now());
            userService.save(seller);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "商家审核通过成功");
            return success(result);
        } catch (Exception e) {
            return error("审核通过失败：" + e.getMessage());
        }
    }
    
    // 审核拒绝商家
    @PostMapping("/reject/{sellerId}")
    public Result<Map<String, Object>> rejectSeller(
            @PathVariable Long sellerId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            User seller = userService.findById(sellerId);
            
            if (seller == null) {
                return error("商家不存在");
            }
            
            if (seller.getUserType() != 1) {
                return error("该用户不是商家");
            }
            
            // 更新审核状态为已拒绝
            String reason = request != null ? request.get("reason") : "未通过审核";
            seller.setIsVerified(2); // 2表示已拒绝
            seller.setVerificationInfo("审核拒绝：" + (reason != null ? reason : "资料不符合要求"));
            seller.setStatus(0); // 禁用账号
            seller.setUpdatedAt(LocalDateTime.now());
            userService.save(seller);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "商家审核拒绝成功");
            return success(result);
        } catch (Exception e) {
            return error("审核拒绝失败：" + e.getMessage());
        }
    }
    
    // 禁用商家
    @PostMapping("/disable/{sellerId}")
    public Result<Map<String, Object>> disableSeller(@PathVariable Long sellerId) {
        try {
            User seller = userService.findById(sellerId);
            
            if (seller == null) {
                return error("商家不存在");
            }
            
            seller.setStatus(0); // 禁用
            seller.setUpdatedAt(LocalDateTime.now());
            userService.save(seller);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "商家禁用成功");
            return success(result);
        } catch (Exception e) {
            return error("禁用商家失败：" + e.getMessage());
        }
    }
    
    // 启用商家
    @PostMapping("/enable/{sellerId}")
    public Result<Map<String, Object>> enableSeller(@PathVariable Long sellerId) {
        try {
            User seller = userService.findById(sellerId);
            
            if (seller == null) {
                return error("商家不存在");
            }
            
            seller.setStatus(1); // 启用
            seller.setUpdatedAt(LocalDateTime.now());
            userService.save(seller);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "商家启用成功");
            return success(result);
        } catch (Exception e) {
            return error("启用商家失败：" + e.getMessage());
        }
    }
    
    // 商家提交审核
    @PostMapping("/submit-audit/{sellerId}")
    public Result<Map<String, Object>> submitAudit(@PathVariable Long sellerId) {
        try {
            User seller = userService.findById(sellerId);
            
            if (seller == null) {
                return error("商家不存在");
            }
            
            if (seller.getUserType() != 1) {
                return error("该用户不是商家");
            }
            
            // 重置审核状态为待审核
            seller.setIsVerified(0);
            seller.setVerificationInfo("已提交审核，请耐心等待");
            seller.setUpdatedAt(LocalDateTime.now());
            userService.save(seller);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "审核提交成功");
            return success(result);
        } catch (Exception e) {
            return error("提交审核失败：" + e.getMessage());
        }
    }
    
    // 获取商家审核历史
    @GetMapping("/audit-history/{sellerId}")
    public Result<List<Map<String, Object>>> getAuditHistory(@PathVariable Long sellerId) {
        try {
            // 这里简化处理，实际应该从audit_log表中查询
            // 返回模拟数据
            List<Map<String, Object>> history = new java.util.ArrayList<>();
            
            User seller = userService.findById(sellerId);
            if (seller != null && seller.getVerificationInfo() != null) {
                Map<String, Object> record = new HashMap<>();
                record.put("action", seller.getIsVerified() == 1 ? "approve" : seller.getIsVerified() == 2 ? "reject" : "pending");
                record.put("remark", seller.getVerificationInfo());
                record.put("adminName", "系统管理员");
                record.put("createdAt", seller.getVerifiedAt() != null ? seller.getVerifiedAt() : seller.getUpdatedAt());
                history.add(record);
            }
            
            return success(history);
        } catch (Exception e) {
            return error("获取审核历史失败：" + e.getMessage());
        }
    }
}
