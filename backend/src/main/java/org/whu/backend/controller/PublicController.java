package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.baidumap.BaiduSuggestionResponseDto;
import org.whu.backend.dto.post.PostDetailDto;
import org.whu.backend.dto.post.PostSummaryDto;
import org.whu.backend.dto.travelpack.PackageDetailDto;
import org.whu.backend.dto.travelpack.PackageSearchRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.service.BaiduMapService;
import org.whu.backend.service.PublicService;

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
    public Result<PackageDetailDto> getPackageDetails(@PathVariable String id) {
        PackageDetailDto packageDetails = publicService.getPackageDetails(id);
        return Result.success(packageDetails);
    }

    // 原来的 searchExternalSpots 接口功能不明确，把它替换成更具体的 "suggestions" 接口
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

    @Operation(summary = "获取已发布的游记列表（分页）")
    @GetMapping
    public Result<PageResponseDto<PostSummaryDto>> getPublishedPosts(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        log.info("访问获取公共游记列表接口, 分页参数: {}", pageRequestDto);
        PageResponseDto<PostSummaryDto> resultPage = publicService.getPublishedPosts(pageRequestDto);
        return Result.success(resultPage);
    }

    @Operation(summary = "获取单篇已发布的游记详情")
    @GetMapping("/{id}")
    public Result<PostDetailDto> getPostDetails(@PathVariable String id) {
        log.info("访问获取公共游记详情接口, ID: {}", id);
        PostDetailDto postDetails = publicService.getPostDetails(id);
        return Result.success(postDetails);
    }
}