package org.whu.backend.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.accounts.Role;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class JwtService {

    @Value("${spring.jwt.secret}")
    private String secret;

    @Value("${spring.jwt.expiration}")
    private long expiration;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    public String generateToken(Account account,long theexpiration) {
        return Jwts.builder()
                .claim("identifier", account.getEmail() != null ? account.getEmail() : account.getPhone())
                .claim("role", account.getRole().name())
                .setSubject(account.getId())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + theexpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    public String generateToken(Account account) {
        return generateToken(account, expiration); // 使用 @Value 注入的默认值
    }

    public Map<String, String> generateTokens(Account account) {
        String accessToken = generateToken(account);     // 15分钟
        String refreshToken = generateToken(account, 7 * 24 * 60 * 60 * 1000); // 7天
        String redisKey = "refreshlist:" + account.getId();
        //redisTemplate.opsForValue().set("refresh:" + account.getId(), refreshToken, 7, TimeUnit.DAYS);
        // 将新 token 添加到列表尾部（保持时间顺序）
        redisTemplate.opsForList().rightPush(redisKey, refreshToken);
        // 裁剪列表，只保留最后 3 个，从列表的倒数第 3 个（-3）到倒数第 1 个（-1）保留，其余删除
        redisTemplate.opsForList().trim(redisKey, -3, -1);
        // 设置过期时间（整个列表的生命周期）
        redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
        redisTemplate.expire("refresh:" + account.getId(), 7, TimeUnit.DAYS);

        System.out.println(refreshToken);
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }


    public String extractAccountIdentifier(String token) {
        return extractClaim(token, claims -> claims.get("identifier", String.class));
    }

    public Role extractAccountRole(String token) {
        return Role.valueOf(extractClaim(token, claims -> claims.get("role", String.class)));
    }

    public String extractAccountId(String token) {
        return parseClaims(token).getSubject();
    }

//    public boolean validateToken(String token, Account account) {
//        final String subject = extractAccountIdentifier(token);
//        return subject.equals(account.getEmail()) && !isTokenExpired(token);
//    }
    public boolean validateToken(String token, Account account) {
        final String identifierInToken = extractAccountIdentifier(token);

        boolean isIdentifierMatch = identifierInToken.equals(account.getEmail()) || identifierInToken.equals(account.getPhone());

        return isIdentifierMatch && !isTokenExpired(token);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token).getBody();
    }

    boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseClaims(token));
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }
}

