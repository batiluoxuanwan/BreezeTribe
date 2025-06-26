package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.route.RouteCreateRequestDto;
import org.whu.backend.dto.route.RouteDetailDto;
import org.whu.backend.dto.route.RouteSummaryDto;
import org.whu.backend.dto.route.RouteUpdateRequestDto;
import org.whu.backend.service.MerchantRouteService;
import org.whu.backend.util.SecurityUtil;

// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用
// 该接口暂时废弃不可用

@Tag(name = "经销商-路线管理", description = "经销商管理自己的路线模板")
@RestController
@RequestMapping("/api/dealer/routes")
@PreAuthorize("hasRole('DISABLED')")
@Slf4j
public class MerchantRouteController {

    @Autowired
    private MerchantRouteService merchantRouteService;

    @Operation(summary = "经销商创建一条新路线", description = "只有经销商可以访问这个接口，按顺序提供景点的uid（不是id！，这个uid是百度地图提供的uid）")
    @PostMapping
    public Result<RouteDetailDto> createRoute(@Valid @RequestBody RouteCreateRequestDto routeCreateRequestDto) {
        String currentDealerId = SecurityUtil.getCurrentUserId();
        log.info("访问新增路线接口, id: {}", currentDealerId);
        RouteDetailDto routeDetailDto = merchantRouteService.createRoute(routeCreateRequestDto, currentDealerId);
        return Result.success(routeDetailDto);
    }


    @Operation(summary = "删除自己的一条路线", description = "只有经销商可以访问这个接口")
    @DeleteMapping("/{id}")
    public Result<?> deleteRoute(@PathVariable String id) {
        String currentDealerId = SecurityUtil.getCurrentUserId();
        log.info("访问删除路线接口, id: {}", currentDealerId);
        merchantRouteService.deleteRoute(id, currentDealerId);
        return Result.success("路线删除成功");
    }

    @Operation(summary = "修改自己的一条路线", description = "只有经销商可以访问这个接口，把新的景点列表按顺序传进去，还是传百度地图的uid")
    @PutMapping("/{id}")
    public Result<RouteDetailDto> updateRoute(@PathVariable String id, @Valid @RequestBody RouteUpdateRequestDto routeUpdateRequestDto) {
        String currentDealerId = SecurityUtil.getCurrentUserId();
        log.info("访问修改路线接口, 经销商id: {}", currentDealerId);
        RouteDetailDto updatedRoute = merchantRouteService.updateRoute(id, routeUpdateRequestDto, currentDealerId);
        return Result.success(updatedRoute);
    }

    @Operation(summary = "获取自己创建的路线列表（分页）", description = "只有经销商可以访问这个接口")
    @GetMapping
    public Result<PageResponseDto<RouteSummaryDto>> getMyRoutes(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        String currentDealerId = SecurityUtil.getCurrentUserId();
        log.info("访问获取自己的路线列表接口, 经销商id: {}", currentDealerId);
        PageResponseDto<RouteSummaryDto> resultPage = merchantRouteService.getMyRoutes(currentDealerId, pageRequestDto);
        return Result.success(resultPage);
    }


    @Operation(summary = "获取自己创建的某条路线的详细信息", description = "只有经销商可以访问这个接口")
    @GetMapping("/{id}")
    public Result<RouteDetailDto> getMyRouteDetails(@PathVariable String id) {
        String currentDealerId = SecurityUtil.getCurrentUserId();
        log.info("访问获取单条路线详情接口,经销商 id: {}", currentDealerId);
        RouteDetailDto routeDetails = merchantRouteService.getMyRouteDetails(id, currentDealerId);
        return Result.success(routeDetails);
    }
}

