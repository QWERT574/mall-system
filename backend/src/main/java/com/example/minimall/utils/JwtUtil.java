package com.example.minimall.utils;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * JWT 工具类，负责令牌的生成、解析与校验。
 *
 * <p>使用 HS256 对称加密算法，密钥通过 application.yml / 环境变量注入，
 * 默认值仅供本地开发快速启动使用。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:minimall2024securejwtkey1234567890abcdef12345678}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expirationTimeMillis;

    private long expirationTime; // 兼容旧代码中使用的毫秒为单位

    @PostConstruct
    public void init() {
        this.expirationTime = this.expirationTimeMillis;
    }

    /**
     * 根据用户对象生成 JWT 令牌。
     *
     * @param user 实现了 getId/getUsername 的用户对象（当前仅支持 {@link com.example.minimall.model.User}）
     * @return 已签名的 JWT 字符串
     */
    public String generateToken(Object user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        
        // 获取用户 ID
        Long userId = null;
        String username = null;
        
        if (user instanceof com.example.minimall.model.User) {
            com.example.minimall.model.User u = (com.example.minimall.model.User) user;
            userId = u.getId();
            username = u.getUsername();
        }
        
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, secret)
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
                .setSigningKey(secret)
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
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 校验 JWT 是否合法（签名正确、未过期、格式正确）。
     *
     * @param token JWT 字符串
     * @return 合法返回 true，否则 false（具体错误写入 stderr 日志）
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
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
