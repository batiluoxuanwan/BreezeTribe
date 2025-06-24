package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.route.RouteCreateRequestDto;
import org.whu.backend.dto.route.RouteSummaryDto;
import org.whu.backend.dto.route.RouteUpdateRequestDto;

@Tag(name = "路线 (Route)", description = "经销商创建和管理路线模板")
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Operation(summary = "经销商创建一条新路线", description = "传入路线中景点的百度地图地图UID列表")
    @PostMapping
    // @PreAuthorize("hasRole('DEALER')") // TODO: 添加权限控制
    public Result<?> createRoute(@RequestBody RouteCreateRequestDto routeCreateRequestDto) {
        // DTO示例: { "name": "...", "spotUids": ["uid1", "uid2"] }
        // TODO: 业务逻辑:
        // 1. 获取当前经销商ID
        // 2. 遍历 spotUids 列表
        // 3. 对每个 uid 执行“查库/请求百度API/入库”的隐式操作，拿到我们自己的Spot实体
        // 4. 创建新的 Route 实体，并关联上这些 Spot 实体
        // 5. 保存 Route 到数据库
        return Result.success();
    }

    @Operation(summary = "经销商删除一条新路线", description = "传入路线的id")
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('DEALER')") // TODO: 添加权限控制
    public Result<?> deleteRoute(@PathVariable String id) {
        // TODO: 删除路线，权限控制
        return Result.success();
    }

    @Operation(summary = "经销商获取自己的路线列表")
    @GetMapping("/my-routes")
    // @PreAuthorize("hasRole('DEALER')") // TODO: 添加权限控制
    public Result<RouteSummaryDto> getMyRoutes(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO: 分页查询实现
        return Result.success();
    }

    @Operation(summary = "经销商修改自己的路线列表")
    @PutMapping("/{id}")
    // @PreAuthorize("hasRole('DEALER')") // TODO: 添加权限控制
    public Result<?> updateRoutes(@RequestBody RouteUpdateRequestDto routeUpdateRequestDto) {
        // TODO: 修改逻辑
        return Result.success();
    }
}