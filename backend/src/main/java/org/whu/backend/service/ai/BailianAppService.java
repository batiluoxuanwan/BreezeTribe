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
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.ai.RecommendedPackageDto;
import org.whu.backend.dto.ai.TagSuggestionRequestDto;
import org.whu.backend.dto.tag.TagDto;
import org.whu.backend.entity.Tag;
import org.whu.backend.repository.TagRepository;
import org.whu.backend.service.DtoConverter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BailianAppService {

    // 从 application.properties 中注入配置项
    @Value("${dashscope.api-key.package-recommend}")
    private String apiKeyForPackageRecommend;
    @Value("${dashscope.app-id.package-recommend}")
    private String appIdForPackageRecommend;

    @Value("${dashscope.api-key.tag-recommend}")
    private String apiKeyForTagRecommend;
    @Value("${dashscope.app-id.tag-recommend}")
    private String appIdForTagRecommend;


    // Spring Boot自带的JSON处理工具，非常强大
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private DtoConverter dtoConverter;

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
                .apiKey(apiKeyForPackageRecommend)
                .appId(appIdForPackageRecommend)
                .prompt(userQuery) // 这里直接传入用户的原始提问
                .build();

        log.info("正在向百炼RAG应用发送请求，AppId: {}, Query: {}", appIdForPackageRecommend, userQuery);

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

    /**
     * 【核心方法】根据文本内容，请求AI推荐相关标签
     *
     * @param requestDto 包含标题和内容的请求
     * @return AI推荐的标签DTO列表
     */
    @Transactional(readOnly = true)
    public List<TagDto> suggestTagsForText(TagSuggestionRequestDto requestDto) {
        // 1. 准备“菜单”：从数据库获取所有可用标签
        List<Tag> allTags = tagRepository.findAll();
        if (allTags.isEmpty()) {
            log.warn("服务层：标签库为空，无法进行AI推荐。");
            return Collections.emptyList();
        }
        String availableTags = allTags.stream().map(Tag::getName).collect(Collectors.joining(", "));
        // 2. 准备“文章”：将标题和内容拼接起来
        String fullText = "标题：" + requestDto.getTitle() + "\n内容：" + requestDto.getContent();
        // 3. 构建精心设计的“指令”(Prompt)
        String prompt = buildPrompt(availableTags, fullText);
        // log.info(prompt);
        // 4. 调用AI服务
        try {
            Application application = new Application();
            ApplicationParam param = ApplicationParam.builder()
                    .apiKey(apiKeyForTagRecommend)
                    .appId(appIdForTagRecommend)
                    .prompt(prompt)
                    .build();
            ApplicationResult result = application.call(param);
            String aiResponse = result.getOutput().getText();
            log.info("服务层：AI大模型返回的原始标签推荐: {}", aiResponse);

            // 5. 解析AI返回的JSON，并从数据库中找出对应的Tag实体
            List<String> recommendedTagNames = parseAiResponse(aiResponse);
            if (recommendedTagNames.isEmpty()) {
                return Collections.emptyList();
            }

            List<Tag> recommendedTags = tagRepository.findByNameIn(recommendedTagNames);

            // 6. 转换为DTO返回给前端
            return recommendedTags.stream()
                    .map(dtoConverter::convertTagToDto)
                    .collect(Collectors.toList());

        } catch (NoApiKeyException | InputRequiredException | ApiException e) {
            log.error("服务层：调用AI打标签服务时发生严重错误！", e);
            return Collections.emptyList();
        }
    }

    /**
     * 私有辅助方法：构建给AI的指令
     */
    private String buildPrompt(String availableTags, String textToTag) {
        return String.format(
                """
                        --- 可选标签列表 ---
                        [%s]
                        
                        --- 需要打标签的文字 ---
                        %s
                        
                        --- 你的回答 ---
                        """,
                availableTags, textToTag
        );
    }

    /**
     * 私有辅助方法：解析AI返回的JSON字符串
     */
    private List<String> parseAiResponse(String response) {
        try {
            return objectMapper.readValue(response, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("服务层：解析AI返回的JSON失败: {}", response, e);
            // TODO: 可以尝试做一些简单的补救，比如提取[]之间的内容，但这里为了简单直接返回空
            return Collections.emptyList();
        }
    }
}