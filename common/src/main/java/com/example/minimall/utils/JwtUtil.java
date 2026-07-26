package com.example.minimall.utils;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类，负责令牌的生成、解析与校验。
 *
 * <p>使用 HS256 对称加密算法，密钥通过 application.yml / 环境变量注入，
 * 默认值仅供本地开发快速启动使用。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expirationTimeMillis;

    private long expirationTime; // 兼容旧代码中使用的毫秒为单位

    @PostConstruct
    public void init() {
        this.expirationTime = this.expirationTimeMillis;
    }

    /**
     * 根据用户 ID 和用户名生成 JWT 令牌。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return 已签名的 JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, secret.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    /**
     * 从令牌中获取用户名（username 声明）。
     *
     * @param token JWT 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
        return claims.get("username", String.class);
    }

    /**
     * 从令牌中获取用户 ID（subject）。
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }

    /**
     * 校验 JWT 是否合法（签名正确、未过期、格式正确）。
     *
     * @param token JWT 字符串
     * @return 合法返回 true，否则 false（具体错误写入 stderr 日志）
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret.getBytes(StandardCharsets.UTF_8)).parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            System.err.println("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            System.err.println("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            System.err.println("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            System.err.println("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            System.err.println("JWT claims string is empty");
        }
        return false;
    }

    /**
     * 获取令牌有效期（毫秒）。
     *
     * @return 过期毫秒数
     */
    public long getExpirationTime() {
        return expirationTime;
    }
}
