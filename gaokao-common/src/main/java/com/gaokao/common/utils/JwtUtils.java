package com.gaokao.common.utils;

import com.gaokao.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
public class JwtUtils {

    /**
     * 默认密钥（生产环境应从配置读取）
     */
    private static final String DEFAULT_SECRET = "gaokao_jwt_secret_key_2024_minimum_32_chars!";

    /**
     * 默认过期时间（毫秒）
     */
    private static final long DEFAULT_EXPIRE_TIME = 24 * 60 * 60 * 1000L;

    /**
     * 生成 Token
     *
     * @param userId 用户 ID
     * @return Token
     */
    public static String generateToken(Long userId) {
        return generateToken(userId, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 生成 Token
     *
     * @param userId    用户 ID
     * @param expireTime 过期时间（毫秒）
     * @return Token
     */
    public static String generateToken(Long userId, long expireTime) {
        SecretKey secretKey = Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8));
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token
     *
     * @param token Token
     * @return Claims
     */
    public static Claims parseToken(String token) {
        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new BusinessException(401, "Token 解析失败");
        }
    }

    /**
     * 获取 Token 中的用户 ID
     *
     * @param token Token
     * @return 用户 ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 验证 Token 是否过期
     *
     * @param token Token
     * @return 是否过期
     */
    public static boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token Token
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}