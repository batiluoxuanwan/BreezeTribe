package org.whu.backend.controller.admin;

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
import org.whu.backend.dto.accounts.MerchantSummaryDto;
import org.whu.backend.dto.admin.RejectionRequestDto;
import org.whu.backend.dto.travelpack.PackageDetailDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.service.admin.AdminService;
import org.whu.backend.util.JpaUtil;

@Tag(name = "管理员-审核中心", description = "处理经销商注册和旅行团发布的审核请求")
@RestController
@RequestMapping("/api/admin/approvals")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApprovalController {

    @Autowired
    private AdminService adminService;

    @Operation(summary = "获取待审核的旅行团列表（分页）")
    @GetMapping("/travel-packages")
    public Result<PageResponseDto<PackageSummaryDto>> getPendingPackages(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        PageResponseDto<PackageSummaryDto> dto = adminService.getPendingPackages(pageRequestDto);
        JpaUtil.notNull(dto, "查询失败");
        return Result.success("查询成功",dto);
    }

    @Operation(summary = "批准一个旅行团")
    @PostMapping("/travel-packages/{packageId}/approve")
    public Result<?> approvePackage(@PathVariable String packageId) {
        JpaUtil.isTrue(adminService.approvePackage(packageId), "操作失败");
        return Result.success("操作成功，旅行团已发布");
    }

    @Operation(summary = "驳回一个旅行团")
    @PostMapping("/travel-packages/{packageId}/reject")
    public Result<?> rejectPackage(@PathVariable String packageId, @RequestBody RejectionRequestDto rejectionDto) {
        JpaUtil.isTrue(adminService.rejectPackage(packageId, rejectionDto), "操作失败");
        return Result.success("操作成功，已驳回该旅行团");
    }

    @Operation(summary = "获取单个旅行团的详情信息", description = "管理员获取用于审核，包含其所有的路线和景点信息")
    @GetMapping("/travel-packages/{packageId}")
    public Result<PackageDetailDto> getPackageDetails(@PathVariable String packageId) {
        PackageDetailDto packageDetails = adminService.getPackageDetails(packageId);
        return Result.success(packageDetails);
    }

    @Operation(summary = "获取待审核的商户列表（分页）")
    @GetMapping("/merchants")
    public Result<PageResponseDto<MerchantSummaryDto>> getPendingMerchants(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        pageRequestDto.setSortBy("createdAt");
        PageResponseDto<MerchantSummaryDto> dto = adminService.getPendingMerchants(pageRequestDto);
        JpaUtil.notNull(dto, "查询失败");
        return Result.success("查询成功",dto);
    }

    @Operation(summary = "批准一个商户注册")
    @PostMapping("/merchants/{packageId}/approve")
    public Result<?> approveMerchants(@PathVariable String packageId) {
        JpaUtil.isTrue(adminService.approveMerchants(packageId), "操作失败");
        return Result.success("操作成功");
    }

    @Operation(summary = "驳回一个商户注册")
    @PostMapping("/merchants/{packageId}/reject")
    public Result<?> rejectMerchants(@PathVariable String packageId, @RequestBody(required = false) RejectionRequestDto rejectionDto) {
        JpaUtil.isTrue(adminService.rejectMerchants(packageId, rejectionDto), "操作失败");
        return Result.success("操作成功");
    }
}