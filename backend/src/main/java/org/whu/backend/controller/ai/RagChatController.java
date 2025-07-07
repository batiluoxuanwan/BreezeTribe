package org.whu.backend.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.whu.backend.common.Result;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.ai.ContentGenerationRequestDto;
import org.whu.backend.dto.ai.ContentGenerationResponseDto;
import org.whu.backend.dto.ai.RecommendedPackageDto;
import org.whu.backend.dto.ai.TagSuggestionRequestDto;
import org.whu.backend.dto.tag.TagDto;
import org.whu.backend.service.ai.BailianAppService;
import java.util.Collections;
import java.util.List;

@Tag(name = "AI智能问答", description = "提供基于RAG的智能旅行推荐，提供标签智能推荐")
@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/chat") // 你可以定义一个更合适的路径
public class RagChatController {

    @Autowired
    private BailianAppService bailianAppService;



    @Operation(summary = "获取智能推荐的旅行团ID列表，传入用户的问题，返回推荐的旅行团的摘要信息和原因")
    @PostMapping("/retrieve-ids")
    public Result<List<RecommendedPackageDto>> getRecommendedPackageIds(@RequestBody String query) {
        if (query == null || query.isEmpty()) {
            throw new BizException("查询语句不能为空");
        }

        // 调用Service层的核心方法
        List<RecommendedPackageDto> packages = bailianAppService.getPackageIdsFromApp(query);

        if (packages.isEmpty()) {
            return Result.success("对不起，暂时没有为您找到合适的旅游团项目，换个问题试试吧。", Collections.emptyList());
        }

        return Result.success("成功检索到相关旅行团ID", packages);
    }

    @Operation(summary = "智能推荐标签", description = """
            根据输入的标题和内容，由AI推荐一批合适的标签。对经销商可以把行程转换成纯文本喂给AI，对游记可以直接投喂。
            例如，经销商想要一些标签，提示经销商先填写好路线信息，旅行团描述，旅行团标题，然后再点击按钮获取AI推荐的标签
            写游记的用户想要获取一些标签，可以提示用户先写好游记的内容，再点击获取AI推荐的标签
            """)
    @PostMapping("/suggest-tags")
    public Result<List<TagDto>> suggestTags(@Valid @RequestBody TagSuggestionRequestDto requestDto) {
        List<TagDto> suggestedTags = bailianAppService.suggestTagsForText(requestDto);
        return Result.success("标签推荐成功", suggestedTags);
    }


    @Operation(summary = "【新增】智能生成内容", description = """
            根据核心文本和角色（商家/用户），由AI生成标题和描述文案。
            ROLE参数是切换提示词，如果是MERCHANT就是适用于商家的营销提示词，如果是USER就是适用于用户的游记提示词
            对于商家，需要商家先填写好路线信息，然后把路线信息转换成纯文本的形式，还可以拼接上商家自己的要求传递给AI
            对于用户，用户可以撰写自己的一些感受想法之类的，还有用户自己的要求，然后传递给AI
            """)
    @PostMapping("/generate-content")
    public Result<ContentGenerationResponseDto> generateContent(@Valid @RequestBody ContentGenerationRequestDto requestDto) {
        ContentGenerationResponseDto generatedContent = bailianAppService.generateContent(requestDto);
        return Result.success("内容生成成功", generatedContent);
    }
}