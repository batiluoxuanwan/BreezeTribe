package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.spot.SpotCreateRequestDto;
import org.whu.backend.dto.spot.SpotSearchResponseDto;
import org.whu.backend.dto.spot.SpotSummaryDto;
import org.whu.backend.dto.spot.SpotUpdateRequestDto;
import org.whu.backend.entity.Spot;

@Tag(name = "景点 (Spot)", description = "提供景点搜索服务及后台管理，即管理员管理已缓存进数据库的景点。同时可以使用此接口转发百度的地点搜索API，在一般情况下正常用户和商户不会调用其中的增删改方法")
@RestController
@RequestMapping("/api/spots")
public class SpotController {

    @Operation(summary = "搜索外部地图平台的景点（百度API）", description = "供前端调用，后端再去请求百度地图API，返回搜索结果列表")
    @GetMapping("/search-external")
    public Result<SpotSearchResponseDto> searchExternalSpots(@RequestParam String keyword) {
        // TODO: 在这里调用百度地图API，并返回一个简化的结果列表给前端
        return Result.success();
    }

    @Operation(summary = "更新景点信息 (管理员权限)")
    @PutMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN')") // TODO: 添加权限控制
    public Result<?> updateSpot(@PathVariable String id, @RequestBody SpotUpdateRequestDto spotUpdateRequestDto) {
        // TODO：进行景点更新操作
        return Result.success();
    }

    @Operation(summary = "创建景点信息 (管理员权限)")
    @PostMapping()
    // @PreAuthorize("hasRole('ADMIN')") // TODO: 添加权限控制
    public Result<?> createSpot(@RequestBody SpotCreateRequestDto spotCreateRequestDto) {
        // TODO：进行景点新增操作
        return Result.success();
    }

    @Operation(summary = "删除景点信息 (管理员权限)")
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN')") // TODO: 添加权限控制
    public Result<?> deleteSpot(@PathVariable String id) {
        // TODO：进行景点删除操作
        return Result.success();
    }

    @Operation(summary = "查询数据库中已存在的景点信息 (管理员权限)", description = "采用分页查询")
    @GetMapping("")
    // @PreAuthorize("hasRole('ADMIN')") // TODO: 添加权限控制
    public Result<PageResponseDto<SpotSummaryDto>> deleteSpot(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO：进行景点分页查询操作
        return Result.success();
    }
}