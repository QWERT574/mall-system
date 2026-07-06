package com.example.minimall.utils;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类，负责令牌的生成、解析与校验。
 *
 * <p>使用 HS256 对称加密算法，密钥与过期时间硬编码在类中（仅供演示，
 * 实际生产环境应放入配置中心并保证密钥长度 ≥ 32 字节）。
 */
@Component
public class JwtUtil {

    // 使用 HS256 算法，密钥长度需要至少 256 位（32 字节）
    private String secret = "minimall2024securejwtkey1234567890abcdef12345678";
    private long expirationTime = 1000 * 60 * 60 * 24; // 24 小时

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
