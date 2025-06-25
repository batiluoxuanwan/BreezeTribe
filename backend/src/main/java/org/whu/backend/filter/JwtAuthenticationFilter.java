package org.whu.backend.filter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.whu.backend.entity.Accounts.Account;
import org.whu.backend.entity.Accounts.Role;
import org.whu.backend.repository.AuthRepository;
import org.whu.backend.service.JwtService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthRepository authRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   AuthRepository authRepository) {
        this.jwtService = jwtService;
        this.authRepository = authRepository;
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
                // 将账号对象构建为 Spring Security 所需要的 Authentication
                var authToken = new UsernamePasswordAuthenticationToken(
                        account, null, /* 可选：account.getAuthorities() */ null
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 将认证对象放入上下文
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}