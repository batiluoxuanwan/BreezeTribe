package org.whu.backend.service.ai;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.ai.*;
import org.whu.backend.dto.tag.TagDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.Tag;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.repository.TagRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.whu.backend.service.DtoConverter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    @Autowired
    private TravelPackageRepository travelPackageRepository;


    /**
     * 根据用户问题，获取包含完整详情的旅行团推荐列表
     * @param userQuery 用户的自然语言查询
     * @return 包含完整摘要信息和推荐理由的DTO列表
     */
    @Transactional(readOnly = true)
    public List<RecommendedPackageDto> getPackageIdsFromApp(String userQuery) {
        // 1. 调用AI的RAG应用，获取包含ID和推荐理由的JSON字符串
        String aiResponseJson = callAiRagApp(userQuery);
        if (aiResponseJson == null || aiResponseJson.isBlank()) {
            return Collections.emptyList();
        }

        // 2. 解析AI返回的JSON，得到一个中间DTO列表
        List<AiRecoIntermediateDto> intermediateRecos = parseAiRecommendResponse(aiResponseJson);
        if (intermediateRecos.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("服务层：成功从AI解析出 {} 条推荐。", intermediateRecos.size());

        // 3. 【性能关键】一次性从数据库中查出所有推荐的旅行团实体
        List<String> packageIds = intermediateRecos.stream().map(AiRecoIntermediateDto::getId).collect(Collectors.toList());
        List<TravelPackage> packages = travelPackageRepository.findAllById(packageIds);

        // 为了方便快速查找，将实体列表转换为Map
        Map<String, TravelPackage> packageMap = packages.stream()
                .collect(Collectors.toMap(TravelPackage::getId, pkg -> pkg));

        // 4. 【数据整合】遍历中间结果，组装成最终的、包含完整信息的DTO列表
        return intermediateRecos.stream()
                .map(reco -> {
                    TravelPackage pkg = packageMap.get(reco.getId());
                    if (pkg != null) {
                        // 复用已有的转换逻辑，将实体转换为摘要DTO
                        PackageSummaryDto summaryDto = dtoConverter.convertPackageToSummaryDto(pkg);
                        // 构建最终的推荐DTO
                        return RecommendedPackageDto.builder()
                                .packageSummaryDto(summaryDto)
                                .reason(reco.getReason())
                                .build();
                    }
                    return null; // 如果根据ID找不到对应的产品，则过滤掉
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 私有辅助方法：调用阿里云百炼RAG应用
     */
    private String callAiRagApp(String userQuery) {
        ApplicationParam param = ApplicationParam.builder()
                .apiKey(apiKeyForPackageRecommend)
                .appId(appIdForPackageRecommend)
                .prompt(userQuery)
                .build();

        log.info("服务层：正在向百炼RAG应用发送请求，AppId: {}, Query: {}", appIdForPackageRecommend, userQuery);
        try {
            ApplicationResult result = new Application().call(param);
            String resultText = result.getOutput().getText();
            log.info("服务层：百炼RAG应用返回原始文本: {}", resultText);
            return resultText;
        } catch (NoApiKeyException | InputRequiredException | ApiException e) {
            log.error("服务层：调用百炼RAG应用时发生严重错误！", e);
            throw new BizException("AI推荐服务暂时不可用，请稍后重试。");
        }
    }

    /**
     * 私有辅助方法：解析AI返回的JSON字符串
     */
    private List<AiRecoIntermediateDto> parseAiRecommendResponse(String response) {
        try {
            return objectMapper.readValue(response, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("服务层：解析AI返回的RAG推荐JSON失败: {}", response, e);
            return Collections.emptyList();
        }
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
                        你是一位经验丰富的旅游内容编辑，你的任务是从一个给定的标签列表中，为一段旅行相关的文字挑选出5到10个最贴切的标签。
                        规则：\
                        1. 你的回答必须是一个JSON数组格式的字符串，例如：["标签1", "标签2", "标签3"]
                        2. 绝对不能包含任何JSON格式之外的文字、解释或代码块标记。
                        3. 只从下面的“可选标签列表”中进行选择。
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
            return Collections.emptyList();
        }
    }


    /**
     * 【核心方法】根据文本和角色，请求AI生成标题和内容
     *
     * @param requestDto 包含核心文本和角色的请求
     * @return 包含标题和内容的响应DTO
     */
    public ContentGenerationResponseDto generateContent(ContentGenerationRequestDto requestDto) {
        // 2. 根据角色，构建新的、更开放的Prompt
        String prompt = buildPrompt(requestDto.getRole(), requestDto.getText());

        // 3. 调用AI服务
        try {
            Application application = new Application();
            ApplicationParam param = ApplicationParam.builder()
                    .apiKey(apiKeyForTagRecommend)
                    .appId(appIdForTagRecommend)
                    .prompt(prompt)
                    .build();

            ApplicationResult result = application.call(param);
            String aiResponseText = result.getOutput().getText();
            log.info("服务层：AI大模型返回的原始文案: {}", aiResponseText);

            // 4. 直接将AI的原始输出设置到DTO中
            ContentGenerationResponseDto responseDto = new ContentGenerationResponseDto();
            responseDto.setContent(aiResponseText);
            return responseDto;

        } catch (NoApiKeyException | InputRequiredException | ApiException e) {
            log.error("服务层：调用AI内容生成服务时发生严重错误！", e);
            // 返回一个默认的错误提示
            ContentGenerationResponseDto errorResponse = new ContentGenerationResponseDto();
            errorResponse.setContent("抱歉，AI助手当前开小差了，请稍后再试。");
            return errorResponse;
        }
    }

    /**
     * 私有辅助方法：根据角色构建不同的指令
     */
    private String buildPrompt(ContentGenerationRequestDto.RequestRole role, String text) {
        if (role == ContentGenerationRequestDto.RequestRole.MERCHANT) {
            // 商家需要的营销推广文案
            return String.format(
                    """
                            你是一位顶级的旅游营销文案专家，你的任务是根据给定的核心信息，创作一篇吸引人的旅行团描述文案和一个旅行团的标题。
                            请先写标题，然后换两行，再写一段大约100-300字左右的推广文案。文案要语言生动活泼，突出亮点，能激发用户的购买欲望。
                            --- 核心信息 ---
                            %s
                            --- 请开始你的创作 ---
                            """,
                    text
            );
        } else { // USER
            // 用户需要的个人游记风格
            return String.format(
                    """
                            你是一位旅行博主小帮手，你的任务是根据给定的游记草稿或关键词，按照用户的要求写成一篇充满个人色彩和真情实感的游记，并起一个风格相符的标题。
                            请先写标题，然后换两行，再写一段大约100-300字左右的游记正文。文笔按照用户的关键词来，如果用户没有给定，则选择符合草稿的语言风格。
                            --- 游记草稿/关键词 ---
                            %s
                            --- 请开始你的创作 ---
                            """,
                    text
            );
        }
    }
}