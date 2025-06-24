package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.spot.SpotCreateRequestDto;
import org.whu.backend.dto.spot.SpotSummaryDto;
import org.whu.backend.dto.spot.SpotUpdateRequestDto;
import org.whu.backend.entity.Spot;

@Tag(name = "管理员-景点管理", description = "管理员维护平台景点缓存数据库")
@RestController
@RequestMapping("/api/admin/spots")
// @PreAuthorize("hasRole('ADMIN')") // TODO: 权限控制
public class AdminSpotController {

    @Operation(summary = "手动创建一个新景点")
    @PostMapping
    public Result<Spot> createSpot(@RequestBody SpotCreateRequestDto spotCreateRequestDto) {
        // TODO: 业务逻辑...
        return Result.success();
    }

    @Operation(summary = "更新一个景点的信息")
    @PutMapping("/{id}")
    public Result<Spot> updateSpot(@PathVariable String id, @RequestBody SpotUpdateRequestDto spotUpdateRequestDto) {
        // TODO: 业务逻辑...
        return Result.success();
    }

    @Operation(summary = "删除一个缓存的景点")
    @DeleteMapping("/{id}")
    public Result<?> deleteSpot(@PathVariable String id) {
        // TODO: 业务逻辑...
        return Result.success();
    }

    @Operation(summary = "分页查询数据库中已存在的景点")
    @GetMapping
    public Result<PageResponseDto<SpotSummaryDto>> getSpots(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO: 业务逻辑...
        return Result.success();
    }
}