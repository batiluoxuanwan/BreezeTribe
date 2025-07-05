package org.whu.backend.controller.merchant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.whu.backend.dto.order.TravelOrderDetailDto;
import org.whu.backend.dto.travelpack.*;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.merchant.MerchantPackageService;
import org.whu.backend.util.AccountUtil;

import java.util.List;

@Tag(name = "经销商-旅行团管理", description = "经销商管理自己的旅行团商品")
@RestController
@Slf4j
@RequestMapping("/api/dealer/travel-packages")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantPackageController {

    @Autowired
    MerchantPackageService merchantPackageService;
    @Autowired
    private DtoConverter dtoConverter;

    @Operation(summary = "创建一个新旅行团", description = "填写一个新的旅行团的相关信息，创建后状态为待审核，其中有一个id列表，按顺序填写所包含的路线")
    @PostMapping
    public Result<PackageDetailDto> createPackage(@Valid @RequestBody PackageCreateRequestDto packageCreateRequestDto) throws InterruptedException {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("经销商ID '{}' 访问创建旅行团接口", currentDealerId);

        TravelPackage createdPackage = merchantPackageService.createPackage(packageCreateRequestDto, currentDealerId);

        // 调用公共服务的方法将实体转换为DTO
        PackageDetailDto dto = dtoConverter.convertPackageToDetailDto(createdPackage);

        return Result.success("旅行团创建成功，请等待管理员审核", dto);
    }

    @Operation(summary = "更新一个旅行团的标签", description = "用新的标签列表完全替换旧的标签列表。传一个空列表则为清空所有标签。")
    @PutMapping("/{packageId}/tags")
    public Result<?> updatePackageTags(
            @Parameter(description = "要更新标签的产品ID") @PathVariable String packageId,
            @RequestBody List<String> tagIds) {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("请求日志：经销商ID '{}' 正在更新产品ID '{}' 的标签", currentDealerId, packageId);

        merchantPackageService.updatePackageTags(packageId, tagIds, currentDealerId);

        return Result.success("产品标签更新成功");
    }

    @Operation(summary = "获取自己的单个旅行团的详情", description = "包含其所有的路线和景点信息，团期信息")
    @GetMapping("/travel-packages/{id}")
    public Result<PackageDetailForMerchantDto> getPackageDetails(@PathVariable String id) {
        String currentMerchantId = AccountUtil.getCurrentAccountId();
        PackageDetailForMerchantDto packageDetails = merchantPackageService.getPackageDetailsForMerchant(id, currentMerchantId);
        return Result.success(packageDetails);
    }

    @Operation(summary = "更新自己的一个旅行团", description = "目前版本只允许更新文本信息，更新后状态变为待审核")
    @PutMapping("/{id}")
    // TODO: 未来可以逐步放开对价格、路线等其他信息的修改
    public Result<PackageDetailDto> updatePackage(@PathVariable String id, @Valid @RequestBody PackageUpdateRequestDto packageUpdateRequestDto) throws InterruptedException {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("经销商ID '{}' 访问更新旅行团接口, ID: {}", currentDealerId, id);

        TravelPackage updatedPackage = merchantPackageService.updatePackage(id, packageUpdateRequestDto, currentDealerId);

        PackageDetailDto dto = dtoConverter.convertPackageToDetailDto(updatedPackage);

        return Result.success("旅行团信息更新成功，请等待管理员审核", dto);
    }

    @Operation(summary = "删除自己的一个旅行团", description = "逻辑删除")
    @DeleteMapping("/{id}")
    public Result<?> deletePackage(@PathVariable String id) {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("经销商ID '{}' 访问删除旅行团接口, ID: {}", currentDealerId, id);
        merchantPackageService.deletePackage(id, currentDealerId);
        return Result.success("旅行团删除成功");
    }

    @Operation(summary = "【新增】检查一个旅行团当前是否可以被修改", description = "用于前端判断是否禁用“编辑”按钮，提升用户体验。")
    @GetMapping("/{packageId}/can-update")
    public Result<CanUpdateStatusDto> getUpdateStatus(@Parameter(description = "要检查的产品ID") @PathVariable String packageId) {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("请求日志：经销商ID '{}' 正在检查产品ID '{}' 是否可被更新", currentDealerId, packageId);
        CanUpdateStatusDto statusDto = merchantPackageService.checkUpdateStatus(packageId, currentDealerId);
        return Result.success("查询成功", statusDto);
    }

    // TODO: 后续要细化已发布的，待审核的，驳回的
    @Operation(summary = "获取自己创建的旅行团列表（分页）")
    @GetMapping
    public Result<PageResponseDto<PackageSummaryDto>> getMyPackages(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("经销商ID '{}' 访问获取自己的旅行团列表接口", currentDealerId);
        PageResponseDto<PackageSummaryDto> resultPage = merchantPackageService.getMyPackages(currentDealerId, pageRequestDto);
        return Result.success(resultPage);
    }

    // 经销商查看订单 ---
    @Operation(summary = "查看指定旅行团的订单列表（分页）", description = "让经销商了解自己产品的销售情况")
    @GetMapping("/{packageId}/orders")
    // 如果需要控制敏感信息，可以改为OrderSummaryForDealer
    public Result<PageResponseDto<TravelOrderDetailDto>> getPackageOrders(
            @PathVariable String packageId,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("经销商ID '{}' 访问获取旅行团 '{}' 的订单列表接口", currentDealerId, packageId);

        PageResponseDto<TravelOrderDetailDto> resultPage = merchantPackageService.getPackageOrders(packageId, currentDealerId, pageRequestDto);

        return Result.success(resultPage);
    }
}