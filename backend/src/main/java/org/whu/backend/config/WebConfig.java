package org.whu.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 应用到所有 /api/ 路径下的请求
                .allowedOrigins("http://localhost:5174",
                        "http://localhost:5173",
                        "http://localhost:8081",
                        "https://121.43.136.251:8080",
                        "https://121.43.136.251",
                        "https://frp-dad.com:36680") // 前端应用的源地址
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 允许的HTTP方法
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true) // 是否允许发送Cookie
                .maxAge(3600); // 预检请求（OPTIONS请求）的缓存时间，单位秒
        registry.addMapping("/ws/**")
                .allowedOrigins("http://localhost:5174",
                        "http://localhost:5173",
                        "http://localhost:8081",
                        "https://121.43.136.251:8080",
                        "https://frp-dad.com:36680") // 前端应用的源地址
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

