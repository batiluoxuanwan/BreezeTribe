package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.spot.SpotSearchResponseDto;
import org.whu.backend.dto.travelpack.PackageSearchRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;

@Tag(name = "公共接口 (Public)", description = "无需认证即可访问的API")
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Operation(summary = "获取已发布的旅行团列表（分页）", description = "供所有用户浏览")
    @GetMapping("/travel-packages")
    public Result<PageResponseDto<PackageSummaryDto>> getPublishedPackages(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO: 查询状态为 PUBLISHED 的旅行团并分页返回
        return Result.success();
    }

    @Operation(summary = "搜索旅行团（复杂条件）", description = "根据多种条件组合搜索旅行团")
    @GetMapping("/travel-packages/search")
    public Result<PageResponseDto<PackageSummaryDto>> searchPackages(@Valid @ParameterObject PackageSearchRequestDto searchRequestDto) {
        // 后端实现提示:
        // 在Service层，可以使用JPA的Specification或Criteria API来动态构建查询条件
        // 这样可以优雅地处理searchRequestDto中各种可选的搜索参数
        // TODO: 实现复杂的、多条件的搜索逻辑
        return Result.success();
    }

    @Operation(summary = "获取单个旅行团的公开详情", description = "包含其路线和景点信息")
    @GetMapping("/travel-packages/{id}")
    public Result<PackageSummaryDto> getPackageDetails(@PathVariable String id) {
        // TODO: 查询旅行团及其关联的路线和景点详情
        return Result.success();
    }

    @Operation(summary = "搜索外部地图平台的景点", description = "用于前端景点选择功能，后端转发百度地图API请求，返回搜索到的一些地名以及详细的信息，例如百度地图uid等")
    @GetMapping("/spots/search-external")
    public Result<SpotSearchResponseDto> searchExternalSpots(@RequestParam String keyword) {
        // TODO: 调用百度地图API，并返回一个简化的结果列表给前端
        return Result.success();
    }
}