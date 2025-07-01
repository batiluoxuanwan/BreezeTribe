package org.whu.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AIWebClientConfig {

    // 从 application.properties 文件中注入你的配置
    @Value("${ai.studio.api.base-url}")
    private String baseUrl;
    @Value("${ai.studio.api.key}")
    private String apiKey;

    @Bean
    public WebClient aiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(baseUrl) // 设置基础URL
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) // 默认请求头
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey) // 默认的认证头
                .build();
    }
}