package com.example.minimall.controller;

import com.example.minimall.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/** 调试用：将 admin 账号密码重置为默认值的接口 */
@RestController
@RequestMapping("/api/debug")
@Profile("dev")
public class DebugResetController {

    /** 密码编码器 */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /** 用户业务服务 */
    @Autowired
    private com.example.minimall.service.UserService userService;

    /** 重置 admin 账号密码为 admin123 */
    @PostMapping("/reset-admin")
    public Result<Map<String, Object>> resetAdminPassword() {
        try {
            Map<String, Object> data = new HashMap<>();
            
            String newPassword = "admin123";
            String encodedPassword = passwordEncoder.encode(newPassword);
            
            com.example.minimall.model.User admin = userService.findByUsername("admin");
            if (admin == null) {
                admin = new com.example.minimall.model.User();
                admin.setUsername("admin");
                admin.setNickname("Administrator");
                admin.setUserType(2);
                admin.setStatus(1);
            }
            
            admin.setPassword(encodedPassword);
            userService.save(admin);
            
            data.put("message", "Admin password reset successfully");
            data.put("username", "admin");
            data.put("password", newPassword);
            data.put("encodedHash", encodedPassword);
            
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Reset failed: " + e.getMessage());
        }
    }
}
