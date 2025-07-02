package org.whu.backend.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.whu.backend.service.auth.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            //String authHeader = httpServletRequest.getHeader("Authorization");
            // ① 优先从 Header 获取 Authorization
            String authHeader = httpServletRequest.getHeader("Authorization");

            // ② 如果没有，再从 URL 参数中尝试读取 token
            if ((authHeader == null || !authHeader.startsWith("Bearer "))) {
                String tokenParam = httpServletRequest.getParameter("token");
                if (tokenParam != null) {
                    authHeader = "Bearer " + tokenParam;
                }
            }

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String accountId = jwtService.extractAccountId(token);

                    // 设置认证上下文，供 Spring Security 使用
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(accountId, null, null);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    attributes.put("userId", accountId); // 可选，供其他用途
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception ex) {
        // 清除上下文
        SecurityContextHolder.clearContext();
    }
}

