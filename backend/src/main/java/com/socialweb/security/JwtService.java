package com.socialweb.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expireMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-days:7}") long expireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireDays * 24 * 60 * 60 * 1000L;
    }

    public String generate(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析 token，非法或过期返回 null */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
