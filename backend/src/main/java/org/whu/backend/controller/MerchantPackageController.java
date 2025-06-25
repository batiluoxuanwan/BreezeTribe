package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.order.OrderSummaryDto;
import org.whu.backend.dto.travelpack.PackageCreateRequestDto;
import org.whu.backend.dto.travelpack.PackageDetailDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.dto.travelpack.PackageUpdateRequestDto;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.service.MerchantPackageService;
import org.whu.backend.service.PublicService;
import org.whu.backend.util.SecurityUtil;

@Tag(name = "经销商-旅行团管理", description = "经销商管理自己的旅行团商品")
@RestController
@Slf4j
@RequestMapping("/api/dealer/travel-packages")
// @PreAuthorize("hasRole('MERCHANT')") // TODO: 权限控制
public class MerchantPackageController {

    @Autowired
    MerchantPackageService merchantPackageService;
    @Autowired
    PublicService publicService;

    @Operation(summary = "创建一个新旅行团", description = "填写一个新的旅行团的相关信息，创建后状态为待审核，其中有一个id列表，按顺序填写所包含的路线")
    @PostMapping
    // @PreAuthorize("hasRole('MERCHANT')")
    public Result<PackageDetailDto> createPackage(@Valid @RequestBody PackageCreateRequestDto packageCreateRequestDto) {
        String currentDealerId = SecurityUtil.getCurrentUserId();
        log.info("经销商ID '{}' 访问创建旅行团接口", currentDealerId);

        TravelPackage createdPackage = merchantPackageService.createPackage(packageCreateRequestDto, currentDealerId);

        // 调用公共服务的方法将实体转换为DTO
        PackageDetailDto dto = publicService.convertPackageToDetailDto(createdPackage);

        return Result.success("旅行团创建成功，请等待管理员审核", dto);
    }

    @Operation(summary = "更新自己的一个旅行团", description = "目前版本只允许更新文本信息，更新后状态变为待审核")
    @PutMapping("/{id}")
    // TODO: 未来可以逐步放开对价格、路线等其他信息的修改
    public Result<PackageDetailDto> updatePackage(@PathVariable String id, @Valid @RequestBody PackageUpdateRequestDto packageUpdateRequestDto) {
        String currentDealerId = SecurityUtil.getCurrentUserId();
        log.info("经销商ID '{}' 访问更新旅行团接口, ID: {}", currentDealerId, id);

        TravelPackage updatedPackage = merchantPackageService.updatePackage(id, packageUpdateRequestDto, currentDealerId);

        PackageDetailDto dto = publicService.convertPackageToDetailDto(updatedPackage);

        return Result.success("旅行团信息更新成功，请等待管理员审核", dto);
    }

    @Operation(summary = "删除自己的一个旅行团", description = "逻辑删除")
    @DeleteMapping("/{id}")
    public Result<?> deletePackage(@PathVariable String id) {
        String currentDealerId = SecurityUtil.getCurrentUserId();
        log.info("经销商ID '{}' 访问删除旅行团接口, ID: {}", currentDealerId, id);
        merchantPackageService.deletePackage(id, currentDealerId);
        return Result.success("旅行团删除成功");
    }

    @Operation(summary = "获取自己创建的旅行团列表（分页）")
    @GetMapping
    public Result<PageResponseDto<PackageSummaryDto>> getMyPackages(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        String currentDealerId = SecurityUtil.getCurrentUserId();
        log.info("经销商ID '{}' 访问获取自己的旅行团列表接口", currentDealerId);
        PageResponseDto<PackageSummaryDto> resultPage = merchantPackageService.getMyPackages(currentDealerId, pageRequestDto);
        return Result.success(resultPage);
    }

    // 经销商查看订单 ---
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