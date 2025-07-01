package org.whu.backend.service.ai;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.ai.RecommendedPackageDto;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class BailianAppService {

    // 从 application.properties 中注入配置项
    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.app-id}")
    private String appId;

    // Spring Boot自带的JSON处理工具，非常强大
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 调用百炼RAG应用，并期望它返回一个包含ID的JSON字符串，然后解析它。
     *
     * @param userQuery 用户的自然语言查询
     * @return 解析后的旅行团ID列表
     */
    public List<RecommendedPackageDto> getPackageIdsFromApp(String userQuery) {
        // 1. 创建百炼应用实例
        Application application = new Application();

        // 2. 构建请求参数
        // 注意：这里的 .prompt() 就是我们精心设计的、要求返回JSON的那个Prompt模板！
        // 百炼平台会自动把用户问题(${query})和知识库内容(${documents})填进去。
        ApplicationParam param = ApplicationParam.builder()
                .apiKey(apiKey)
                .appId(appId)
                .prompt(userQuery) // 这里直接传入用户的原始提问
                .build();

        log.info("正在向百炼RAG应用发送请求，AppId: {}, Query: {}", appId, userQuery);

        try {
            // 3. 发起调用
            ApplicationResult result = application.call(param);
            String resultText = result.getOutput().getText();
            log.info("百炼RAG应用返回原始文本: {}", resultText);

            // 4. 【最关键也最脆弱的一步】解析AI返回的文本为ID列表
            return parseResultToDtoList(resultText);

        } catch (NoApiKeyException | InputRequiredException | ApiException e) {
            log.error("调用百炼RAG应用时发生严重错误！", e);
            // 抛出一个业务异常，让全局异常处理器去捕获并返回统一的错误格式给前端
            throw new BizException("AI服务调用失败，请稍后重试或联系管理员。");
        }
    }

    /**
     * 私有辅助方法：尝试将AI返回的字符串解析为 List<RecommendedPackageDto>
     */
    private List<RecommendedPackageDto> parseResultToDtoList(String text) {
        if (text == null || text.isBlank() || text.contains("对不起")) {
            return Collections.emptyList();
        }

        // ... (之前写的寻找'['和']'的逻辑依然有用) ...
        int startIndex = text.indexOf('[');
        int endIndex = text.lastIndexOf(']');

        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            String jsonArrayStr = text.substring(startIndex, endIndex + 1);
            try {
                // 解析为DTO列表
                return objectMapper.readValue(jsonArrayStr, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                log.error("解析百炼返回的DTO JSON字符串失败！", e);
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }
}