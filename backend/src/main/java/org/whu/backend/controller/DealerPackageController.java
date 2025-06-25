package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.order.OrderSummaryDto;
import org.whu.backend.dto.travelpack.PackageCreateRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.dto.travelpack.PackageUpdateRequestDto;
import org.whu.backend.entity.TravelPackage;

@Tag(name = "经销商-旅行团管理", description = "经销商管理自己的旅行团商品")
@RestController
@RequestMapping("/api/dealer/travel-packages")
// @PreAuthorize("hasRole('DEALER')") // TODO: 权限控制
public class DealerPackageController {

    @Operation(summary = "创建一个新旅行团", description = "创建后状态为待审核，等待管理员审核")
    @PostMapping
    public Result<TravelPackage> createPackage(@RequestBody PackageCreateRequestDto packageCreateRequestDto) {
        // TODO: 创建旅行团，并将状态设置为 PENDING_APPROVAL
        return Result.success();
    }

    @Operation(summary = "更新自己的一个旅行团", description = "创建后状态为待审核，等待管理员审核")
    @PutMapping("/{id}")
    public Result<TravelPackage> updatePackage(@PathVariable String id, @RequestBody PackageUpdateRequestDto packageUpdateRequestDto) {
        // TODO: 更新旅行团，并将状态设置为 PENDING_APPROVAL
        return Result.success();
    }

    @Operation(summary = "删除自己的一个旅行团", description = "逻辑删除")
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

    // --- [新增] 经销商查看订单 ---
    @Operation(summary = "查看指定旅行团的订单列表（分页）", description = "让经销商了解自己产品的销售情况")
    @GetMapping("/{packageId}/orders")
    // 如果需要控制敏感信息，可以改为OrderSummaryForDealer
    public Result<PageResponseDto<OrderSummaryDto>> getPackageOrders(
            @PathVariable String packageId,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        // TODO: 查询与该旅行团关联的、已支付的订单列表
        // 注意：出于隐私保护，只返回必要的、非敏感信息给经销商
        return Result.success();
    }
}