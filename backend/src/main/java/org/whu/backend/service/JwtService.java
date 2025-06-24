package org.whu.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.whu.backend.entity.Accounts.Account;
import org.whu.backend.entity.Accounts.Role;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtService {
    @Value("${spring.jwt.secret}")
    private String secret;

    @Value("${spring.jwt.expiration}")
    private long expiration;

    public String generateToken(Account account) {
        return Jwts.builder()
                .claim("identifier", account.getEmail() != null ? account.getEmail() : account.getPhone())
                .claim("role", account.getRole().name())
                .setSubject(account.getId().toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractAccountIdentifier(String token) {
        return extractClaim(token, claims -> claims.get("identifier", String.class));
    }

    public Role extractAccountRole(String token) {
        return Role.valueOf(extractClaim(token, claims -> claims.get("role", String.class)));
    }

    public boolean validateToken(String token, Account account) {
        final String subject = extractAccountIdentifier(token);
        return subject.equals(account.getEmail()) && !isTokenExpired(token);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token).getBody();
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseClaims(token));
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

