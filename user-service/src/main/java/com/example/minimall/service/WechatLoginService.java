package com.example.minimall.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 微信小程序登录服务 — 将前端传来的 code 换成服务端 openid。
 *
 * <p>双模式：
 * <ul>
 *   <li>生产模式：配置 wechat.appid + wechat.secret 后，调用微信 jscode2session API 用 code 换真实 openid</li>
 *   <li>开发模式：未配置 appid 时，openid = "wxdev_" + SHA256(code + jwtSecret)[:16]，稳定且不可预测</li>
 * </ul>
 *
 * <p>安全要点（C8 修复）：
 * <ol>
 *   <li>openid 必须由服务端生成，<b>绝不接受前端直传</b>，否则攻击者可传任意 openid 登录任意账号</li>
 *   <li>开发模式下同一 code 生成同一 openid（稳定），避免每次登录重复创建用户</li>
 *   <li>openid 依赖 jwtSecret 派生，攻击者无法仅凭 code 反推或伪造他人 openid</li>
 * </ol>
 *
 * <p>生产部署：在 .env / 环境变量配置 WECHAT_APPID / WECHAT_SECRET 即自动切换生产模式。
 */
@Slf4j
@Service
public class WechatLoginService {

    @Value("${wechat.appid:}")
    private String appid;

    @Value("${wechat.secret:}")
    private String secret;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (isProductionMode()) {
            log.info("WechatLoginService 运行于【生产模式】—— 调用微信 jscode2session 换取真实 openid");
        } else {
            log.warn("WechatLoginService 运行于【开发模式】—— 未配置 wechat.appid，openid 由 code 经 HMAC 派生（仅供开发，生产必须配置 appid/secret）");
        }
    }

    private boolean isProductionMode() {
        return appid != null && !appid.isEmpty() && secret != null && !secret.isEmpty();
    }

    /**
     * 用微信 code 换取 openid。
     *
     * @param code 微信小程序登录凭证（前端 wx.login 获得）
     * @return 服务端生成的 openid
     * @throws IllegalArgumentException code 为空
     * @throws RuntimeException         微信 API 调用失败
     */
    public String code2Openid(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("微信登录 code 不能为空");
        }
        return isProductionMode() ? code2OpenidFromWechat(code) : code2OpenidDev(code);
    }

    /**
     * 生产模式：调用微信 jscode2session 接口换取真实 openid。
     * 文档：https://developers.weixin.qq.com/miniprogram/dev/api-backend/open-api/login/auth.code2Session.html
     */
    private String code2OpenidFromWechat(String code) {
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, secret, code);
        try {
            String resp = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(resp);
            int errcode = node.path("errcode").asInt(0);
            if (errcode != 0) {
                String errmsg = node.path("errmsg").asText("unknown");
                log.error("微信 code2session 失败: errcode={}, errmsg={}", errcode, errmsg);
                throw new RuntimeException("微信登录失败: " + errmsg);
            }
            String openid = node.path("openid").asText(null);
            if (openid == null || openid.isEmpty()) {
                throw new RuntimeException("微信返回 openid 为空");
            }
            return openid;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信 jscode2session 异常", e);
            throw new RuntimeException("微信登录服务异常: " + e.getMessage());
        }
    }

    /**
     * 开发模式：openid = "wxdev_" + SHA256(jwtSecret + code)[:16hex]
     * 同一 code 稳定映射同一 openid，且不可预测（依赖 jwtSecret）。
     */
    private String code2OpenidDev(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(jwtSecret.getBytes(StandardCharsets.UTF_8));
            md.update(code.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) { // 取前 8 字节 = 16 个 hex 字符
                sb.append(String.format("%02x", hash[i]));
            }
            return "wxdev_" + sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
    }
}
