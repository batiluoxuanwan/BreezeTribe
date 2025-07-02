package org.whu.backend.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.whu.backend.dto.ai.AIChunkResponseDto;
import org.whu.backend.dto.ai.AIRequestDto;
import org.whu.backend.dto.ai.MessageDto;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@Slf4j
public class AIService {

    @Autowired
    private WebClient aiWebClient; // 注入我们刚刚配置好的WebClient

    @Autowired
    private ObjectMapper objectMapper; // Spring Boot自动配置的JSON处理工具

    @Value("${ai.studio.model.name}")
    private String modelName; // 从配置文件读取模型名称

    /**
     * [核心] 调用AI模型获取一个完整的、非流式的回答
     * @param userPrompt 用户的提问
     * @return AI生成的完整回答字符串
     */
    public String getSimpleCompletion(String userPrompt) {
        log.info("AIService: 正在为提问 '{}' 请求AI回答...", userPrompt);

        // 1. 构建请求体
        AIRequestDto requestDto = AIRequestDto.builder()
                .model(modelName)
                .messages(List.of(new MessageDto("user", userPrompt)))
                .temperature(0.6)
                .stream(false) // [注意] 这里我们请求非流式输出，更简单
                .build();

        // 2. 发送请求并处理响应
        // .bodyToMono(String.class) 会把整个响应体作为一个字符串来接收
        // .block() 会阻塞当前线程，直到收到完整的响应。对于简单请求来说很方便。
        String responseJson = aiWebClient.post()
                .uri("/chat/completions") // 请求的具体路径
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("AIService: 收到AI的原始响应: {}", responseJson);

        // 3. 从复杂的JSON响应中，解析出我们真正需要的那段回答文字
        try {
            // 这里我们需要一个临时的DTO来解析非流式响应的结构
            var responseDto = objectMapper.readValue(responseJson, NonStreamingResponse.class);
            if (responseDto != null && !responseDto.getChoices().isEmpty()) {
                String content = responseDto.getChoices().get(0).getMessage().getContent();
                log.info("AIService: 解析出的回答内容: {}", content);
                return content;
            }
        } catch (Exception e) {
            log.error("AIService: 解析AI响应失败", e);
        }

        return "抱歉，AI服务暂时无法回答您的问题。";
    }

    // 临时的内部类，用于解析非流式响应
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NonStreamingResponse {
        private List<Choice> choices;
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {
            private MessageDto message;
        }
    }

    /**
     * [新增] 调用AI模型并获取流式回答
     * @param userPrompt 用户的提问
     * @return 一个包含AI逐字回答的文本流 (Flux)
     */
    public Flux<String> getStreamingCompletion(String userPrompt) {
        log.info("AIService: 正在为提问 '{}' 请求AI流式回答...", userPrompt);

        AIRequestDto requestDto = AIRequestDto.builder()
                .model(modelName)
                .messages(List.of(new MessageDto("user", userPrompt)))
                .temperature(0.6)
                .stream(true) // [关键] 设置为true来开启流式输出
                .build();

        // .bodyToFlux(String.class) 会将响应体作为文本行的数据流来处理
        return aiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestDto)
                .retrieve()
                .bodyToFlux(String.class)
                // 对每一行数据进行处理
                .mapNotNull(line -> {
                    // AI的流式响应通常以 "data: " 开头，我们需要把它去掉
                    if (line.startsWith("data: ")) {
                        String jsonChunk = line.substring(6);
                        // 结束标志
                        if ("[DONE]".equals(jsonChunk)) {
                            return null;
                        }
                        // 解析JSON数据块
                        try {
                            AIChunkResponseDto chunk = objectMapper.readValue(jsonChunk, AIChunkResponseDto.class);
                            if (chunk != null && !chunk.getChoices().isEmpty()) {
                                // 使用正确的getter方法
                                if ("stop".equals(chunk.getChoices().get(0).getFinish_reason())) {
                                    return null;
                                }
                                // 返回真正的文字内容
                                return chunk.getChoices().get(0).getDelta().getContent();
                            }
                        } catch (Exception e) {
                            log.error("AIService: 解析流式数据块失败, line: {}", line, e);
                            return null;
                        }
                    }
                    return null; // 忽略不是以 "data: " 开头的行
                });
    }
}