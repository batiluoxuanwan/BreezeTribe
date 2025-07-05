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

    @Operation(summary = "获取智能推荐的旅行团ID列表")
    @PostMapping("/retrieve-ids")
    public Result<List<RecommendedPackageDto>> getRecommendedPackageIds(@RequestBody String query) {
        if (query == null || query.isEmpty()) {
            throw new BizException("查询语句不能为空");
        }

        // 调用Service层的核心方法
        List<RecommendedPackageDto> packageIds = bailianAppService.getPackageIdsFromApp(query);

        if (packageIds.isEmpty()) {
            return Result.success("对不起，暂时没有为您找到合适的旅游团项目，换个问题试试吧。", Collections.emptyList());
        }

        return Result.success("成功检索到相关旅行团ID", packageIds);
    }

    @Operation(summary = "智能推荐标签", description = "根据输入的标题和内容，由AI推荐一批合适的标签。")
    @PostMapping("/suggest-tags")
    public Result<List<TagDto>> suggestTags(@Valid @RequestBody TagSuggestionRequestDto requestDto) {
        List<TagDto> suggestedTags = bailianAppService.suggestTagsForText(requestDto);
        return Result.success("标签推荐成功", suggestedTags);
    }
}