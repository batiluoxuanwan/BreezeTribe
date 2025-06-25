package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.whu.backend.entity.Route;
import org.whu.backend.service.MerchantRouteService;

@Tag(name = "经销商-路线管理", description = "经销商管理自己的路线模板")
@RestController
@RequestMapping("/api/dealer/routes")
//@PreAuthorize("hasRole('MERCHANT')") // TODO: 可以在类级别上统一进行权限控制
public class MerchantRouteController {

    @Autowired
    private MerchantRouteService merchantRouteService;

    @Operation(summary = "经销商创建一条新路线")
    @PostMapping
    // @PreAuthorize("hasRole('MERCHANT')")
    public Result<RouteSummaryDto> createRoute(@Valid @RequestBody RouteCreateRequestDto routeCreateRequestDto) {
        // TODO: 待鉴权模块完成后，从SecurityContext获取当前登录的经销商ID
        String currentDealerId = "42e5daa8-ffcb-49b2-b210-92739e0b9803"; // 写死的UUID占位符

        RouteSummaryDto routeSummaryDto = merchantRouteService.createRoute(routeCreateRequestDto, currentDealerId);

        // TODO: 可以将 createdRoute 转换为一个更适合返回给前端的 RouteDetailDto
        return Result.success(routeSummaryDto);
    }

    @Operation(summary = "删除自己的一条路线")
    @DeleteMapping("/{id}")
    public Result<?> deleteRoute(@PathVariable String id) {
        // TODO: 业务逻辑...
        return Result.success("123321");
    }

    @Operation(summary = "修改自己的一条路线")
    @PutMapping("/{id}")
    public Result<RouteDetailDto> updateRoute(@PathVariable String id, @RequestBody RouteUpdateRequestDto routeUpdateRequestDto) {
        // TODO: 业务逻辑...
        return Result.success();
    }

    @Operation(summary = "获取自己创建的路线列表（分页）")
    @GetMapping
    public Result<PageResponseDto<RouteSummaryDto>> getMyRoutes(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO: 业务逻辑...
        return Result.success();
    }
}

