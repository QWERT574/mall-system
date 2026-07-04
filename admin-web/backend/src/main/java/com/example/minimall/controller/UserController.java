package com.example.minimall.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.minimall.mapper.ShippingAddressMapper;
import com.example.minimall.mapper.UserMapper;
import com.example.minimall.model.Role;
import com.example.minimall.model.ShippingAddress;
import com.example.minimall.model.User;
import com.example.minimall.service.UserService;
import com.example.minimall.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户相关接口
 * 提供用户登录、信息维护、收货地址增删改查、角色分配、商家资料、账号注销等能力
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /** 用户 Mapper */
    private final UserMapper userMapper;
    /** 收货地址 Mapper */
    private final ShippingAddressMapper shippingAddressMapper;
    /** 用户业务服务 */
    private final UserService userService;
    /** JWT 工具，用于从请求中解析用户身份 */
    private final JwtUtil jwtUtil;
    
    public UserController(UserMapper userMapper, ShippingAddressMapper shippingAddressMapper, UserService userService, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.shippingAddressMapper = shippingAddressMapper;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }
    
    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Map<String,String> body){
        try {
            String code = body.get("code");
            // 使用微信code作为openid的基础，确保同一个微信用户每次登录得到相同的openid
            // 在开发环境中，code可以作为固定标识；生产环境中应调用微信API换取真实openid
            String openid = "openid_" + (code != null && !code.isEmpty() ? code : "default_user");
            String token = Base64.getEncoder().encodeToString((openid+":token").getBytes());
            
            // 检查用户是否存在，不存在则创建
            User user = userMapper.selectByOpenid(openid);
            if (user == null) {
                user = new User();
                user.setOpenid(openid);
                userMapper.insert(user);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("openid", openid);
            result.put("token", token);
            result.put("user", user);
            return createSuccessResponse(result);
        } catch (Exception e) {
            logger.error("登录失败", e);
            return createErrorResponse("登录失败: " + e.getMessage());
        }
    }
    
    // 获取用户信息
    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@RequestParam(required = false) String openid, HttpServletRequest request) {
        try {
            User user = null;
            if (openid != null && !openid.isEmpty()) {
                user = userMapper.selectByOpenid(openid);
            } else {
                Long userId = getUserIdFromToken(request);
                if (userId != null) {
                    user = userMapper.selectById(userId);
                }
            }
            if (user == null) {
                return createErrorResponse("用户不存在");
            }
            return createSuccessResponse(user);
        } catch (Exception e) {
            logger.error("获取用户信息失败", e);
            return createErrorResponse("获取用户信息失败: " + e.getMessage());
        }
    }

    // 修改密码
    @PutMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            String oldPassword = (String) params.get("oldPassword");
            String newPassword = (String) params.get("newPassword");

            User user = userMapper.selectById(userId);
            if (user == null) {
                return createErrorResponse("用户不存在");
            }
            // 验证旧密码
            if (user.getPassword() != null && !user.getPassword().equals(oldPassword)) {
                return createErrorResponse("旧密码不正确");
            }
            // 更新密码
            user.setPassword(newPassword);
            userMapper.updateById(user);
            return createSuccessResponse("密码修改成功");
        } catch (Exception e) {
            logger.error("修改密码失败", e);
            return createErrorResponse("修改密码失败: " + e.getMessage());
        }
    }

    // 更新用户信息
    @PostMapping("/update")
    public Map<String, Object> updateUserInfo(@RequestBody User user) {
        try {
            userMapper.updateById(user);
            return createSuccessResponse(userMapper.selectById(user.getId()));
        } catch (Exception e) {
            logger.error("更新用户信息失败", e);
            return createErrorResponse("更新用户信息失败: " + e.getMessage());
        }
    }
    
    // 获取用户收货地址列表
    @GetMapping("/address/list")
    public Map<String, Object> getAddressList(@RequestParam Long userId) {
        try {
            QueryWrapper<ShippingAddress> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId).orderByDesc("is_default", "id");
            List<ShippingAddress> addresses = shippingAddressMapper.selectList(queryWrapper);
            return createSuccessResponse(addresses);
        } catch (Exception e) {
            logger.error("获取收货地址列表失败", e);
            return createErrorResponse("获取收货地址列表失败: " + e.getMessage());
        }
    }
    
    // 获取用户默认收货地址
    @GetMapping("/address/default")
    public Map<String, Object> getDefaultAddress(@RequestParam Long userId) {
        try {
            QueryWrapper<ShippingAddress> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId).eq("is_default", 1).last("LIMIT 1");
            ShippingAddress address = shippingAddressMapper.selectOne(queryWrapper);
            return createSuccessResponse(address);
        } catch (Exception e) {
            logger.error("获取默认收货地址失败", e);
            return createErrorResponse("获取默认收货地址失败: " + e.getMessage());
        }
    }
    
    // 添加收货地址
    @PostMapping("/address/add")
    public Map<String, Object> addAddress(@RequestBody ShippingAddress address) {
        try {
            // 如果是默认地址，先将其他地址设为非默认
            if (address.getIsDefault() != null && address.getIsDefault()) {
                QueryWrapper<ShippingAddress> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("user_id", address.getUserId());
                List<ShippingAddress> addresses = shippingAddressMapper.selectList(queryWrapper);
                for (ShippingAddress addr : addresses) {
                    addr.setIsDefault(false);
                    shippingAddressMapper.updateById(addr);
                }
            } else {
                address.setIsDefault(false);
            }
            shippingAddressMapper.insert(address);
            return createSuccessResponse(address);
        } catch (Exception e) {
            logger.error("添加收货地址失败", e);
            return createErrorResponse("添加收货地址失败: " + e.getMessage());
        }
    }
    
    // 更新收货地址
    @PostMapping("/address/update")
    public Map<String, Object> updateAddress(@RequestBody ShippingAddress address) {
        try {
            // 如果是默认地址，先将其他地址设为非默认
            if (address.getIsDefault() != null && address.getIsDefault()) {
                QueryWrapper<ShippingAddress> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("user_id", address.getUserId()).ne("id", address.getId());
                List<ShippingAddress> addresses = shippingAddressMapper.selectList(queryWrapper);
                for (ShippingAddress addr : addresses) {
                    addr.setIsDefault(false);
                    shippingAddressMapper.updateById(addr);
                }
            }
            shippingAddressMapper.updateById(address);
            return createSuccessResponse(address);
        } catch (Exception e) {
            logger.error("更新收货地址失败", e);
            return createErrorResponse("更新收货地址失败: " + e.getMessage());
        }
    }
    
    // 删除收货地址
    @PostMapping("/address/delete/{id}")
    public Map<String, Object> deleteAddress(@PathVariable Long id) {
        try {
            shippingAddressMapper.deleteById(id);
            return createSuccessResponse(null);
        } catch (Exception e) {
            logger.error("删除收货地址失败", e);
            return createErrorResponse("删除收货地址失败：" + e.getMessage());
        }
    }
    
    // 获取收货地址详情
    @GetMapping("/address/detail/{id}")
    public Map<String, Object> getAddressDetail(@PathVariable Long id) {
        try {
            ShippingAddress address = shippingAddressMapper.selectById(id);
            if (address == null) {
                return createErrorResponse("地址不存在");
            }
            return createSuccessResponse(address);
        } catch (Exception e) {
            logger.error("获取收货地址详情失败", e);
            return createErrorResponse("获取收货地址详情失败：" + e.getMessage());
        }
    }
    
    // 构建成功响应
    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }
    
    // 用户头像上传接口
    @PostMapping("/uploadAvatar")
    public Map<String, Object> uploadAvatar(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return createErrorResponse("文件不能为空");
            }
            
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String uploadDir = "src/main/resources/static/images/";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            java.io.File dest = new java.io.File(uploadDir + fileName);
            file.transferTo(dest);
            
            String avatarUrl = "/images/" + fileName;
            
            Map<String, Object> result = new HashMap<>();
            result.put("avatar", avatarUrl);
            
            return createSuccessResponse(result);
        } catch (Exception e) {
            logger.error("上传头像失败", e);
            return createErrorResponse("上传头像失败: " + e.getMessage());
        }
    }

    // 构建错误响应
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 1);
        response.put("message", message);
        return response;
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
    
    // 获取所有用户列表，支持分页和筛选
    @GetMapping("/list")
    public Map<String, Object> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userType,
            @RequestParam(required = false) Integer status) {
        try {
            // 使用分页查询，支持筛选
            com.baomidou.mybatisplus.core.metadata.IPage<User> resultPage = userService.findUsersWithPagination(page, pageSize, keyword, userType, status);
            
            // 构建分页响应数据
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("records", resultPage.getRecords());
            pageData.put("total", resultPage.getTotal());
            pageData.put("current", resultPage.getCurrent());
            pageData.put("size", resultPage.getSize());
            
            return createSuccessResponse(pageData);
        } catch (Exception e) {
            logger.error("获取用户列表失败", e);
            return createErrorResponse("获取用户列表失败: " + e.getMessage());
        }
    }
    
    // 根据角色ID获取用户列表
    @GetMapping("/list/role/{roleId}")
    public Map<String, Object> getUserListByRole(@PathVariable Long roleId) {
        try {
            List<User> users = userService.findByRoleId(roleId);
            return createSuccessResponse(users);
        } catch (Exception e) {
            logger.error("获取用户列表失败", e);
            return createErrorResponse("获取用户列表失败: " + e.getMessage());
        }
    }
    
    // 获取所有角色
    @GetMapping("/roles")
    public Map<String, Object> getRoles() {
        try {
            List<Role> roles = userService.findAllRoles();
            return createSuccessResponse(roles);
        } catch (Exception e) {
            logger.error("获取角色列表失败", e);
            return createErrorResponse("获取角色列表失败: " + e.getMessage());
        }
    }
    
    // 为用户分配角色
    @PostMapping("/assign-role")
    public Map<String, Object> assignRole(@RequestBody Map<String, Long> body) {
        try {
            Long userId = body.get("userId");
            Long roleId = body.get("roleId");
            userService.assignRole(userId, roleId);
            return createSuccessResponse(null);
        } catch (Exception e) {
            logger.error("分配角色失败", e);
            return createErrorResponse("分配角色失败: " + e.getMessage());
        }
    }
    
    // 获取用户角色
    @GetMapping("/{id}/role")
    public Map<String, Object> getUserRole(@PathVariable Long id) {
        try {
            Role role = userService.findUserRole(id);
            return createSuccessResponse(role);
        } catch (Exception e) {
            logger.error("获取用户角色失败", e);
            return createErrorResponse("获取用户角色失败: " + e.getMessage());
        }
    }
    
    // 注册商品提供方
    @PostMapping("/register-supplier")
    public Map<String, Object> registerSupplier(@RequestBody User user) {
        try {
            user.setUserType(1); // 设置为商品提供方
            user.setIsVerified(0); // 初始状态为未认证
            userService.save(user);
            return createSuccessResponse(user);
        } catch (Exception e) {
            logger.error("注册商品提供方失败", e);
            return createErrorResponse("注册商品提供方失败: " + e.getMessage());
        }
    }
    
    // 更新用户类型
    @PostMapping("/{id}/update-type")
    public Map<String, Object> updateUserType(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                return createErrorResponse("用户不存在");
            }
            user.setUserType(body.get("userType"));
            userService.save(user);
            return createSuccessResponse(user);
        } catch (Exception e) {
            logger.error("更新用户类型失败", e);
            return createErrorResponse("更新用户类型失败: " + e.getMessage());
        }
    }
    
    // 更新用户状态
    @PostMapping("/{id}/update-status")
    public Map<String, Object> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                return createErrorResponse("用户不存在");
            }
            user.setStatus(body.get("status"));
            userService.save(user);
            return createSuccessResponse(user);
        } catch (Exception e) {
            logger.error("更新用户状态失败", e);
            return createErrorResponse("更新用户状态失败: " + e.getMessage());
        }
    }
    
    // 获取商家信息
    @GetMapping("/seller/{id}")
    public Map<String, Object> getSellerInfo(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                return createErrorResponse("商家不存在");
            }
            if (user.getUserType() != 1) {
                return createErrorResponse("该用户不是商家");
            }
            return createSuccessResponse(user);
        } catch (Exception e) {
            logger.error("获取商家信息失败", e);
            return createErrorResponse("获取商家信息失败: " + e.getMessage());
        }
    }
    
    // 更新商家信息
    @PostMapping("/update-seller")
    public Map<String, Object> updateSellerInfo(@RequestBody User user) {
        try {
            User existingUser = userService.findById(user.getId());
            if (existingUser == null) {
                return createErrorResponse("商家不存在");
            }
            if (existingUser.getUserType() != 1) {
                return createErrorResponse("该用户不是商家");
            }
            userService.save(user);
            return createSuccessResponse(user);
        } catch (Exception e) {
            logger.error("更新商家信息失败", e);
            return createErrorResponse("更新商家信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/deactivate")
    public Map<String, Object> deactivateOwnAccount(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return createErrorResponse("未登录");
            }
            User user = userMapper.selectById(userId);
            if (user == null) {
                return createErrorResponse("用户不存在");
            }
            shippingAddressMapper.delete(new QueryWrapper<ShippingAddress>().eq("user_id", userId));
            userMapper.deleteById(userId);
            return createSuccessResponse(null);
        } catch (Exception e) {
            logger.error("注销账号失败", e);
            return createErrorResponse("注销账号失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/deactivate")
    public Map<String, Object> deactivateAccount(@PathVariable Long id) {
        try {
            User user = userMapper.selectById(id);
            if (user == null) {
                return createErrorResponse("用户不存在");
            }
            if (user.getUserType() != null && user.getUserType() == 2) {
                return createErrorResponse("不能注销管理员账号");
            }
            shippingAddressMapper.delete(new QueryWrapper<ShippingAddress>().eq("user_id", id));
            userMapper.deleteById(id);
            return createSuccessResponse(null);
        } catch (Exception e) {
            logger.error("注销账号失败", e);
            return createErrorResponse("注销账号失败: " + e.getMessage());
        }
    }
}
