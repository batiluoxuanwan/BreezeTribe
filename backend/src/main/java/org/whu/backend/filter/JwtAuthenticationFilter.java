package org.whu.backend.filter;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.accounts.Role;
import org.whu.backend.repository.authRepo.AuthRepository;
import org.whu.backend.service.auth.JwtService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthRepository authRepository;
    private final RedisTemplate<String, String> redisTemplate; // ✅ 注入 RedisTemplate

    public JwtAuthenticationFilter(JwtService jwtService,
                                   AuthRepository authRepository,
                                   RedisTemplate<String, String> redisTemplate) {
        this.jwtService = jwtService;
        this.authRepository = authRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        String authHeader = request.getHeader("Authorization");
        String tokenPrefix = "Bearer ";

        // 检查是否携带 Token
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(tokenPrefix)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(tokenPrefix.length());

        // 检查 token 是否在黑名单中
        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"登录状态已失效，请重新登录\"}");
            return;
        }

        String identifier = jwtService.extractAccountIdentifier(token);
        Role role = jwtService.extractAccountRole(token);

        if (identifier == null || role == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Account> accountOpt = authRepository.findByEmailAndRole(identifier, role);
        if (accountOpt.isEmpty()) {
            accountOpt = authRepository.findByPhoneAndRole(identifier, role);
        }

        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            // 检查 Token 合法性
            if (jwtService.validateToken(token, account)) {
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(account.getRole().name()));
                // 将账号对象构建为 Spring Security 所需要的 Authentication
                var authToken = new UsernamePasswordAuthenticationToken(
                        account, null, authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 将认证对象放入上下文
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}