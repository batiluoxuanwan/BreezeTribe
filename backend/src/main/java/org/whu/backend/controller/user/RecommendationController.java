package org.whu.backend.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.whu.backend.common.Result;
import org.whu.backend.dto.post.PostSummaryDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.service.user.RecommendationService;
import org.whu.backend.util.AccountUtil;

import java.util.List;

@Tag(name = "推荐系统", description = "为用户提供个性化内容推荐")
@RestController
@RequestMapping("/api/user/recommendations")
@PreAuthorize("hasRole('USER')")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Operation(summary = "获取个性化【旅游团】推荐", description = "根据用户画像推荐旅游产品。")
    @GetMapping("/packages")
    public Result<List<PackageSummaryDto>> getPackageRecommendations() {
        String currentUserId = AccountUtil.getCurrentAccountId();
        List<PackageSummaryDto> recommendations = recommendationService.getRecommendedPackages(currentUserId);
        return Result.success("获取旅游团推荐成功", recommendations);
    }

    @Operation(summary = "获取个性化【游记】推荐", description = "根据用户画像推荐游记。")
    @GetMapping("/posts")
    public Result<List<PostSummaryDto>> getPostRecommendations() {
        String currentUserId = AccountUtil.getCurrentAccountId();
        List<PostSummaryDto> recommendations = recommendationService.getRecommendedPosts(currentUserId);
        return Result.success("获取游记推荐成功", recommendations);
    }
}