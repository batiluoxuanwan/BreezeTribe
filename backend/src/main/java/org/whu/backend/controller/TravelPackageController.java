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
import org.whu.backend.dto.travelpack.PackageCreateRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.dto.travelpack.PackageUpdateRequestDto;

@Tag(name = "旅行团 (Travel Package)", description = "最终旅行团商品的管理与用户浏览")
@RestController
@RequestMapping("/api/travel-packages")
public class TravelPackageController {

    @Operation(summary = "经销商创建一个旅行团", description = "传入旅行团信息和选定的路线ID列表")
    @PostMapping
    // @PreAuthorize("hasRole('DEALER')") // TODO: 权限控制
    public Result<?> createPackage(@RequestBody PackageCreateRequestDto packageCreateRequestDto) {
        // DTO示例: { "title": "...", "price": "...", "routeIds": ["uuid1", "uuid2"] }
        // TODO: 创建逻辑
        return Result.success();
    }

    @Operation(summary = "经销商删除一个旅行团", description = "传入旅行团的id")
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('DEALER')") // TODO: 权限控制
    public Result<?> deletePackage(@PathVariable String id) {
        // TODO: 删除逻辑
        return Result.success();
    }

    @Operation(summary = "经销商修改一个旅行团", description = "传入旅行团的id，修改后的信息")
    @PutMapping("/{id}")
    // @PreAuthorize("hasRole('DEALER')") // TODO: 权限控制
    public Result<?> updatePackage(@PathVariable String id, @RequestBody PackageUpdateRequestDto packageUpdateRequestDto) {
        // TODO: 修改逻辑
        return Result.success();
    }

    @Operation(summary = "经销商查找自己的旅行团", description = "传入分页查询参数")
    @GetMapping("/my-packages")
    // @PreAuthorize("hasRole('DEALER')") // TODO: 权限控制
    public Result<PageResponseDto<PackageSummaryDto>> getMyPackage(@PathVariable String id, @RequestBody PackageUpdateRequestDto packageUpdateRequestDto) {
        // TODO: 分页查询自己的旅行团逻辑
        return Result.success();
    }

    @Operation(summary = "【公共】获取旅行团列表（分页）", description = "供所有用户浏览已发布的旅行团")
    @GetMapping
    public Result<PageResponseDto<PackageSummaryDto>> getPublishedPackages(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO: 查找逻辑
        return Result.success();
    }

    @Operation(summary = "【公共】获取单个旅行团详情", description = "包含其所有的路线和景点信息")
    @GetMapping("/{id}")
    public Result<?> getPackageDetails(@PathVariable String id) {
        // TODO：详细信息返回
        return Result.success();
    }
}