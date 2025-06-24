package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.travelpack.PackageCreateRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.dto.travelpack.PackageUpdateRequestDto;
import org.whu.backend.entity.TravelPackage;

@Tag(name = "经销商-旅行团管理", description = "经销商管理自己的旅行团商品")
@RestController
@RequestMapping("/api/dealer/travel-packages")
// @PreAuthorize("hasRole('DEALER')") // TODO: 权限控制
public class DealerPackageController {

    @Operation(summary = "创建一个新旅行团")
    @PostMapping
    public Result<TravelPackage> createPackage(@RequestBody PackageCreateRequestDto packageCreateRequestDto) {
        // TODO: 业务逻辑...
        return Result.success();
    }

    @Operation(summary = "更新自己的一个旅行团")
    @PutMapping("/{id}")
    public Result<TravelPackage> updatePackage(@PathVariable String id, @RequestBody PackageUpdateRequestDto packageUpdateRequestDto) {
        // TODO: 业务逻辑...
        return Result.success();
    }

    @Operation(summary = "删除自己的一个旅行团")
    @DeleteMapping("/{id}")
    public Result<?> deletePackage(@PathVariable String id) {
        // TODO: 业务逻辑...
        return Result.success();
    }

    @Operation(summary = "获取自己创建的旅行团列表（分页）")
    @GetMapping
    public Result<PageResponseDto<PackageSummaryDto>> getMyPackages(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO: 业务逻辑...
        return Result.success();
    }
}