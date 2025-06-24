package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.route.RouteCreateRequestDto;
import org.whu.backend.dto.route.RouteDetailDto;
import org.whu.backend.dto.route.RouteSummaryDto;
import org.whu.backend.dto.route.RouteUpdateRequestDto;

@Tag(name = "经销商-路线管理", description = "经销商管理自己的路线模板")
@RestController
@RequestMapping("/api/dealer/routes")
// @PreAuthorize("hasRole('DEALER')") // TODO: 可以在类级别上统一进行权限控制
public class DealerRouteController {

    @Operation(summary = "创建一条新路线")
    @PostMapping
    public Result<RouteDetailDto> createRoute(@RequestBody RouteCreateRequestDto routeCreateRequestDto) {
        // TODO: 业务逻辑...
        return Result.success();
    }

    @Operation(summary = "删除自己的一条路线")
    @DeleteMapping("/{id}")
    public Result<?> deleteRoute(@PathVariable String id) {
        // TODO: 业务逻辑...
        return Result.success();
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

