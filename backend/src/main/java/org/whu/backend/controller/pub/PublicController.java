package org.whu.backend.controller.pub;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.accounts.ShareDto;
import org.whu.backend.dto.accounts.UserProfileDto;
import org.whu.backend.dto.baidumap.BaiduSuggestionResponseDto;
import org.whu.backend.dto.packagecomment.PackageCommentDto;
import org.whu.backend.dto.post.PostDetailDto;
import org.whu.backend.dto.post.PostSearchRequestDto;
import org.whu.backend.dto.post.PostSummaryDto;
import org.whu.backend.dto.postcomment.PostCommentDto;
import org.whu.backend.dto.postcomment.PostCommentWithRepliesDto;
import org.whu.backend.dto.travelpack.DepartureSummaryDto;
import org.whu.backend.dto.travelpack.PackageDetailDto;
import org.whu.backend.dto.travelpack.PackageSearchRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.service.BaiduMapService;
import org.whu.backend.service.pub.PublicService;
import org.whu.backend.service.user.UserPackageCommentService;
import org.whu.backend.service.user.UserPostCommentService;
import org.whu.backend.util.IpUtil;

import java.util.List;

@Tag(name = "公共接口 (Public)", description = "无需认证即可访问的API，可以获取旅行团列表，搜索旅行团，查看旅行团的详细，转发百度地图API请求查询景点")
@Slf4j
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private PublicService publicService;
    @Autowired
    private BaiduMapService baiduMapService;
    @Autowired
    private UserPostCommentService userPostCommentService;
    @Autowired
    private UserPackageCommentService userPackageCommentService;

    // TODO: 分页查询要对排序字段进行检查
    @Operation(summary = "获取已发布的旅行团列表（分页）", description = "供所有用户浏览")
    @GetMapping("/travel-packages")
    public Result<PageResponseDto<PackageSummaryDto>> getPublishedPackages(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        log.info("收到查询已发布旅行团列表请求");
        PageResponseDto<PackageSummaryDto> resultPage = publicService.getPublishedPackages(pageRequestDto);
        return Result.success(resultPage);
    }


    @Operation(summary = "搜索旅行团（复杂条件）", description = "根据多种条件组合搜索旅行团")
    @GetMapping("/travel-packages/search")
    public Result<PageResponseDto<PackageSummaryDto>> searchPackages(@Valid @ParameterObject PackageSearchRequestDto searchRequestDto) {
        log.info("访问搜索公共游记列表接口,关键词: {}，城市: {}", searchRequestDto.getKeyword(), searchRequestDto.getCity());
        PageResponseDto<PackageSummaryDto> resultPage = publicService.searchPackages(searchRequestDto);
        return Result.success(resultPage);
    }

    @Operation(summary = "获取单个旅行团的公开详情", description = "包含其所有的路线和景点信息")
    @GetMapping("/travel-packages/{id}")
    public Result<PackageDetailDto> getPackageDetails(@PathVariable String id, HttpServletRequest request) {
        // 调用工具类获取真实IP地址
        String ipAddress = IpUtil.getRealIp(request);
        PackageDetailDto packageDetails = publicService.getPackageDetails(id, ipAddress);
        return Result.success(packageDetails);
    }

    @Operation(summary = "获取指定产品的可报名团期列表（分页）", description = "查询一个产品下所有可供用户选择报名的出发团期。")
    @GetMapping("/travel-packages/{packageId}/departures")
    public Result<PageResponseDto<DepartureSummaryDto>> getAvailableDepartures(
            @Parameter(description = "产品模板的ID") @PathVariable String packageId,
            @Valid @ParameterObject PageRequestDto pageRequestDto) {
        log.info("请求日志：收到查询产品ID '{}' 的可报名团期列表请求, 分页参数: {}", packageId, pageRequestDto);
        PageResponseDto<DepartureSummaryDto> resultPage = publicService.getAvailableDeparturesForPackage(packageId, pageRequestDto);
        return Result.success("团期列表查询成功", resultPage);
    }

    @Operation(summary = "获取地点输入提示", description = "用于前端搜索框的实时输入提示，后端转发百度地图API请求")
    @GetMapping("/spots/suggestions")
    public Result<List<BaiduSuggestionResponseDto.SuggestionResult>> getSpotSuggestions(
            @RequestParam String keyword,
            @RequestParam String region
    ) {
        log.info("收到地点输入提示转发请求");
        List<BaiduSuggestionResponseDto.SuggestionResult> suggestions = baiduMapService.getSuggestions(keyword, region);
        return Result.success(suggestions);
    }

    // 游记的复杂搜索接口
    @Operation(summary = "搜索游记（复杂条件）", description = "根据关键词、标签等多种条件组合搜索游记")
    @GetMapping("/posts/search")
    public Result<PageResponseDto<PostSummaryDto>> searchPosts(@Valid @ParameterObject PostSearchRequestDto searchDto) {
        log.info("访问游记搜索接口, 搜索条件: {}", searchDto);
        PageResponseDto<PostSummaryDto> resultPage = publicService.searchPosts(searchDto);
        return Result.success(resultPage);
    }

    @Operation(summary = "获取已发布的游记列表（分页）")
    @GetMapping("posts")
    public Result<PageResponseDto<PostSummaryDto>> getPublishedPosts(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        log.info("访问获取公共游记列表接口, 分页参数: {}", pageRequestDto);
        PageResponseDto<PostSummaryDto> resultPage = publicService.getPublishedPosts(pageRequestDto);
        return Result.success(resultPage);
    }

    @Operation(summary = "获取单篇已发布的游记详情")
    @GetMapping("/posts/{postId}")
    public Result<PostDetailDto> getPostDetails(@PathVariable String postId, HttpServletRequest request) {
        // 调用工具类获取真实IP地址
        String ipAddress = IpUtil.getRealIp(request);
        log.info("访问获取公共游记详情接口, ID: {}", postId);
        PostDetailDto postDetails = publicService.getPostDetails(postId, ipAddress);
        return Result.success(postDetails);
    }

    @Operation(summary = "获取指定游记的评论列表（带少量回复预览）")
    @GetMapping("/posts/{postId}/comments")
    public Result<PageResponseDto<PostCommentWithRepliesDto>> getComments(
            @PathVariable String postId,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        log.info("正在获取游记 '{}' 的评论列表...", postId);
        PageResponseDto<PostCommentWithRepliesDto> resultPage = userPostCommentService.getCommentsByPost(postId, pageRequestDto);
        return Result.success(resultPage);
    }

    @Operation(summary = "获取单条评论的所有回复列表（楼中楼详情）")
    @GetMapping("/posts/comments/{commentId}/replies")
    public Result<PageResponseDto<PostCommentDto>> getCommentReplies(
            @PathVariable String commentId,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        log.info("正在获取评论 '{}' 的所有回复列表...", commentId);
        PageResponseDto<PostCommentDto> resultPage = userPostCommentService.getCommentReplies(commentId, pageRequestDto);
        return Result.success(resultPage);
    }

    @Operation(summary = "获取指定旅行团的评价列表（分页）")
    @GetMapping("/packages/comments/{packageId}")
    public Result<PageResponseDto<PackageCommentDto>> getPackageComments(
            @PathVariable String packageId,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        log.info("正在获取旅行团 '{}' 的评价列表...", packageId);
        PageResponseDto<PackageCommentDto> resultPage = userPackageCommentService.getPackageComments(packageId, pageRequestDto);
        return Result.success(resultPage);
    }

    // 获取单条旅行团评价的直接回复列表（分页）
    @Operation(summary = "获取单条旅行团评价的直接回复列表（分页）")
    @GetMapping("/package-comments/{commentId}/replies")
    public Result<PageResponseDto<PackageCommentDto>> getPackageCommentReplies(
            @PathVariable String commentId,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        log.info("正在获取旅行团评价 '{}' 的直接回复列表...", commentId);
        PageResponseDto<PackageCommentDto> resultPage = userPackageCommentService.getPackageCommentDetails(commentId, pageRequestDto);
        return Result.success(resultPage);
    }

    @Operation(summary = "获取用户的公开主页信息")
    @GetMapping("/users/{userId}/profile")
    public Result<UserProfileDto> getUserProfile(@PathVariable String userId) {
        log.info("访问获取用户 '{}' 的主页信息接口", userId);
        UserProfileDto userProfile = publicService.getUserProfile(userId);
        return Result.success(userProfile);
    }

    @Operation(summary = "获取指定用户发布的游记列表（分页）")
    @GetMapping("/users/{userId}/posts")
    public Result<PageResponseDto<PostSummaryDto>> getUserPosts(
            @PathVariable String userId,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        log.info("访问获取用户 '{}' 的游记列表接口", userId);
        PageResponseDto<PostSummaryDto> resultPage = publicService.getUserPosts(userId, pageRequestDto);
        return Result.success(resultPage);
    }
    @Operation(summary = "获取指定用户发的基础信息")
    @GetMapping("/users/{userId}/info")
    public Result<ShareDto> getUserInfos(
            @PathVariable String userId) {
        log.info("访问获取用户 '{}' 的信息接口", userId);
        ShareDto dto = publicService.getUserInfos(userId);
        System.out.println("YES/n");
        return Result.success("喜报",dto);
    }
}