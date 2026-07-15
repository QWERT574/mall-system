package com.example.minimall.controller;

import com.example.minimall.annotation.RateLimit;
import com.example.minimall.common.Result;
import com.example.minimall.model.User;
import com.example.minimall.service.SmsService;
import com.example.minimall.service.UserService;
import com.example.minimall.utils.JwtUtil;
import com.example.minimall.utils.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 用户认证授权相关接口（登录/注册/密码/短信等） */
@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    /** JWT 工具 */
    @Autowired
    private JwtUtil jwtUtil;

    /** 用户业务服务 */
    @Autowired
    private UserService userService;

    /** 短信服务 */
    @Autowired
    private SmsService smsService;

    /** 密码编码器 */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /** 登出接口（前端清掉 token 即可） */
    @PostMapping("/logout")
    public Result<Map<String, Object>> logout() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "退出登录成功");
        return success(data);
    }

    /** 根据 Authorization 头获取当前登录用户信息 */
    @GetMapping("/user")
    public Result<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return error("未登录");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return error("无效token");
            }
            User user = userService.findById(userId);
            if (user == null) {
                return error("用户不存在");
            }
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("nickname", user.getNickname());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("phone", user.getPhone());
            userInfo.put("userType", user.getUserType());
            userInfo.put("status", user.getStatus());
            userInfo.put("isVerified", user.getIsVerified());
            userInfo.put("companyName", user.getCompanyName());
            return success(userInfo);
        } catch (Exception e) {
            return error("获取用户信息失败: " + e.getMessage());
        }
    }

    /** 通用登录：支持用户名密码登录与微信小程序登录 */
    @RateLimit(limit = 10, timeout = 60)
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");
            String openid = loginRequest.get("openid");
            String code = loginRequest.get("code");
            String nickname = loginRequest.get("nickname");
            String avatar = loginRequest.get("avatar");
            // 图形验证码（用户名密码登录时必填）
            String captchaKey = loginRequest.get("captchaKey");
            String captchaCode = loginRequest.get("captchaCode");

            User user = null;

            // 用户名密码登录
            if (username != null && password != null) {
                // 校验图形验证码
                if (captchaKey == null || captchaKey.isEmpty() || captchaCode == null || captchaCode.isEmpty()) {
                    return error("请输入图形验证码");
                }
                int capResult = CaptchaController.verify(captchaKey, captchaCode);
                if (capResult == 1) {
                    return error("验证码已过期，请刷新后重试");
                }
                if (capResult == 2) {
                    return error("验证码错误");
                }

                // 先尝试通过用户名查找
                user = userService.findByUsername(username);
                // 如果找不到，尝试通过手机号查找
                if (user == null) {
                    user = userService.findByPhone(username);
                }
                
                if (user == null) {
                    return error("用户不存在");
                }
                
                if (user.getPassword() == null) {
                    return error("该账号未设置密码");
                }
                
                if (!passwordEncoder.matches(password, user.getPassword())) {
                    return error("密码错误");
                }

                if (user.getStatus() != null && user.getStatus() == -1) {
                    return error("该账号已注销，无法登录");
                }

                if (user.getStatus() != null && user.getStatus() == 0) {
                    return error("该账号已被禁用，无法登录");
                }
            }
            // 微信小程序登录
            else if (openid != null || code != null) {
                // 如果传入了 code，则使用 code 作为 openid（实际应该调用微信 API 获取 openid）
                if ((openid == null || openid.isEmpty()) && (code != null && !code.isEmpty())) {
                    openid = code;
                }
                
                if (openid == null || openid.isEmpty()) {
                    return error("openid 或 code 不能为空");
                }

                // 检查用户是否存在，如果不存在则创建新用户
                user = userService.findByOpenid(openid);
                if (user == null) {
                    user = new User();
                    user.setOpenid(openid);
                    user.setNickname(nickname != null && !nickname.isEmpty() ? nickname : "微信用户");
                    user.setAvatar(avatar);
                    user.setUserType(0); // 默认买家
                    user.setStatus(1); // 正常状态
                    userService.save(user);
                } else {
                    if (user.getStatus() != null && user.getStatus() == -1) {
                        return error("该账号已注销，无法登录");
                    }
                    if (user.getStatus() != null && user.getStatus() == 0) {
                        return error("该账号已被禁用，无法登录");
                    }
                }
            }
            else {
                return error("请提供登录凭证");
            }

            // 生成 JWT 令牌
            String token = jwtUtil.generateToken(user);

            // 返回用户信息和令牌
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);
            data.put("expiration", jwtUtil.getExpirationTime());
            return success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return error("登录失败：" + e.getMessage());
        }
    }

    /** 发送手机短信验证码 */
    @RateLimit(limit = 3, timeout = 60)
    @GetMapping("/sendCode")
    public Result<Map<String, Object>> sendCode(@RequestParam String phone) {
        Map<String, Object> data = smsService.sendCode(phone);
        if (Boolean.TRUE.equals(data.get("ok"))) {
            return success(data);
        } else {
            return error((String) data.get("message"));
        }
    }

    /** 获取密码强度规则列表，供前端实时展示 */
    @GetMapping("/passwordRules")
    public Result<List<Map<String, String>>> passwordRules() {
        List<Map<String, String>> rules = new java.util.ArrayList<>();
        for (PasswordValidator.Rule r : PasswordValidator.Rule.values()) {
            Map<String, String> entry = new java.util.HashMap<>();
            entry.put("key", r.name());
            entry.put("label", r.label);
            rules.add(entry);
        }
        return success(rules);
    }

    /** 手机号 + 短信验证码登录 */
    @RateLimit(limit = 10, timeout = 60)
    @PostMapping("/loginByCode")
    public Result<Map<String, Object>> loginByCode(@RequestBody Map<String, String> loginRequest) {
        try {
            String phone = loginRequest.get("phone");
            String code = loginRequest.get("code");

            if (phone == null || phone.isEmpty() || code == null || code.isEmpty()) {
                return error("手机号和验证码不能为空");
            }

            // 验证验证码
            int verifyResult = smsService.verifyCode(phone, code);
            if (verifyResult != SmsService.VERIFY_OK) {
                String msg = "验证码错误";
                if (verifyResult == SmsService.VERIFY_EMPTY) msg = "请先获取验证码";
                else if (verifyResult == SmsService.VERIFY_EXPIRED) msg = "验证码已过期";
                return error(msg);
            }

            // 检查用户是否存在，不存在则创建
            User user = userService.findByPhone(phone);
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setUserType(0);
                user.setStatus(1);
                user.setNickname("用户" + phone.substring(Math.max(0, phone.length() - 4)));
                user.setPassword("123456");
                userService.save(user);
            } else {
                if (user.getStatus() != null && user.getStatus() == -1) {
                    return error("该账号已注销，无法登录");
                }
                if (user.getStatus() != null && user.getStatus() == 0) {
                    return error("该账号已被禁用，无法登录");
                }
            }

            // 生成 JWT 令牌
            String token = jwtUtil.generateToken(user);

            // 返回用户信息和令牌
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);
            data.put("expiration", jwtUtil.getExpirationTime());
            return success(data);
        } catch (Exception e) {
            return error("登录失败：" + e.getMessage());
        }
    }

    /** 微信登录用户绑定手机号 */
    @PostMapping("/bindPhone")
    public Result<Map<String, Object>> bindPhone(@RequestBody Map<String, String> request) {
        try {
            String openid = request.get("openid");
            String phone = request.get("phone");
            String code = request.get("code");

            if (openid == null || openid.isEmpty() || phone == null || phone.isEmpty() || code == null || code.isEmpty()) {
                return error("openid、手机号和验证码不能为空");
            }

            // 验证验证码
            int verifyResult = smsService.verifyCode(phone, code);
            if (verifyResult != SmsService.VERIFY_OK) {
                String msg = "验证码错误";
                if (verifyResult == SmsService.VERIFY_EMPTY) msg = "请先获取验证码";
                else if (verifyResult == SmsService.VERIFY_EXPIRED) msg = "验证码已过期";
                return error(msg);
            }

            // 检查用户是否存在
            User user = userService.findByOpenid(openid);
            if (user == null) {
                return error("用户不存在");
            }

            // 检查手机号是否已被其他用户绑定
            User existingUser = userService.findByPhone(phone);
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                return error("该手机号已被其他用户绑定");
            }

            // 绑定手机号
            user.setPhone(phone);
            userService.save(user);

            // 生成新的 JWT 令牌
            String token = jwtUtil.generateToken(user);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);
            data.put("expiration", jwtUtil.getExpirationTime());
            return success(data);
        } catch (Exception e) {
            return error("绑定失败：" + e.getMessage());
        }
    }

    /** 手机号注册账号（支持普通用户与商家） */
    @RateLimit(limit = 3, timeout = 60)
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> request) {
        try {
            String phone = (String) request.get("phone");
            String code = (String) request.get("code");
            String openid = (String) request.get("openid");
            String nickname = (String) request.get("nickname");
            String avatar = (String) request.get("avatar");
            String username = (String) request.get("username");
            String password = (String) request.get("password");
            Integer userType = request.get("userType") != null ? Integer.parseInt(request.get("userType").toString()) : 0;

            if (phone == null || phone.isEmpty()) {
                return error("手机号不能为空");
            }

            if (userType < 0 || userType > 1) {
                return error("无效的用户类型");
            }

            // 短信验证码必填
            if (code == null || code.isEmpty()) {
                return error("请输入短信验证码");
            }
            int capResult = smsService.verifyCode(phone, code);
            if (capResult == SmsService.VERIFY_EMPTY) {
                return error("请先获取验证码");
            }
            if (capResult == SmsService.VERIFY_EXPIRED) {
                return error("验证码已过期，请重新获取");
            }
            if (capResult == SmsService.VERIFY_WRONG) {
                return error("验证码错误");
            }

            // 检查手机号是否已被注册
            User existingUser = userService.findByPhone(phone);
            if (existingUser != null) {
                return error("该手机号已被注册");
            }

            // 检查用户名是否已被使用
            if (username != null && !username.isEmpty()) {
                User existingUserByUsername = userService.findByUsername(username);
                if (existingUserByUsername != null) {
                    return error("该用户名已被使用");
                }
            }

            // 密码复杂度校验
            if (password == null || password.isEmpty()) {
                return error("请输入密码");
            }
            String pwdError = PasswordValidator.validate(password, username, phone);
            if (pwdError != null) {
                return error(pwdError);
            }

            // 创建新用户
            User user = new User();
            user.setPhone(phone);
            user.setOpenid(openid);
            user.setUsername(username);
            if (password != null && !password.isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
            }
            user.setNickname(nickname != null ? nickname : "用户" + phone.substring(phone.length() - 4));
            user.setAvatar(avatar);
            user.setUserType(userType);
            user.setStatus(1); // 默认启用状态
            user.setIsVerified(userType == 1 ? 0 : 1); // 商家需要审核，普通用户默认已审核
            userService.save(user);

            // 生成 JWT 令牌
            String token = jwtUtil.generateToken(user);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);
            data.put("expiration", jwtUtil.getExpirationTime());
            return success(data);
        } catch (Exception e) {
            return error("注册失败：" + e.getMessage());
        }
    }

    /** 通过手机短信验证码重置登录密码 */
    @RateLimit(limit = 3, timeout = 60)
    @PostMapping("/resetPassword")
    public Result<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String phone = request.get("phone");
            String code = request.get("code");
            String password = request.get("password");

            if (phone == null || phone.isEmpty() || code == null || code.isEmpty() || password == null || password.isEmpty()) {
                return error("手机号、验证码和密码不能为空");
            }

            // 密码复杂度校验
            String pwdError = PasswordValidator.validate(password, null, phone);
            if (pwdError != null) {
                return error(pwdError);
            }

            // 验证验证码
            int verifyResult = smsService.verifyCode(phone, code);
            if (verifyResult != SmsService.VERIFY_OK) {
                String msg = "验证码错误";
                if (verifyResult == SmsService.VERIFY_EMPTY) msg = "请先获取验证码";
                else if (verifyResult == SmsService.VERIFY_EXPIRED) msg = "验证码已过期";
                return error(msg);
            }

            // 检查用户是否存在
            User user = userService.findByPhone(phone);
            if (user == null) {
                return error("用户不存在");
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(password));
            userService.save(user);

            Map<String, Object> data = new HashMap<>();
            data.put("message", "密码重置成功");
            return success(data);
        } catch (Exception e) {
            return error("重置密码失败：" + e.getMessage());
        }
    }
}
