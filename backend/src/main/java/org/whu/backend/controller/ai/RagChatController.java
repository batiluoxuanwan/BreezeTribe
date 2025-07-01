package org.whu.backend.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.whu.backend.common.Result;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.service.ai.BailianAppService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Tag(name = "AI智能问答", description = "提供基于RAG的智能旅行推荐")
@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/chat") // 你可以定义一个更合适的路径
public class RagChatController {

    @Autowired
    private BailianAppService bailianAppService;

    @Operation(summary = "获取智能推荐的旅行团ID列表")
    @PostMapping("/retrieve-ids")
    public Result<List<String>> getRecommendedPackageIds(@RequestBody String query) {
        if (query == null || query.isEmpty()) {
            throw new BizException("查询语句不能为空");
        }

        // 调用Service层的核心方法
        List<String> packageIds = bailianAppService.getPackageIdsFromApp(query);

        if (packageIds.isEmpty()) {
            return Result.success("对不起，暂时没有为您找到合适的旅游团项目，换个问题试试吧。", Collections.emptyList());
        }

        return Result.success("成功检索到相关旅行团ID", packageIds);
    }
}