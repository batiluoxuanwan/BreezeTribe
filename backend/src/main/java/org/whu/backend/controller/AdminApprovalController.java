package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.admin.RejectionRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;

@Tag(name = "管理员-审核中心", description = "处理经销商注册和旅行团发布的审核请求")
@RestController
@RequestMapping("/api/admin/approvals")
// @PreAuthorize("hasRole('ADMIN')")
public class AdminApprovalController {

    @Operation(summary = "获取待审核的旅行团列表（分页）")
    @GetMapping("/travel-packages")
    public Result<PageResponseDto<PackageSummaryDto>> getPendingPackages(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO: 查询状态为 PENDING_APPROVAL 的旅行团
        return Result.success();
    }

    @Operation(summary = "批准一个旅行团")
    @PostMapping("/travel-packages/{packageId}/approve")
    public Result<?> approvePackage(@PathVariable String packageId) {
        // TODO: 将旅行团状态从 PENDING_APPROVAL 改为 PUBLISHED
        return Result.success("操作成功，旅行团已发布");
    }

    @Operation(summary = "驳回一个旅行团")
    @PostMapping("/travel-packages/{packageId}/reject")
    public Result<?> rejectPackage(@PathVariable String packageId, @RequestBody RejectionRequestDto rejectionDto) {
        // TODO: 将旅行团状态从 PENDING_APPROVAL 改为 REJECTED，并记录驳回原因
        return Result.success("操作成功，已驳回该旅行团");
    }

    // TODO: 未来可以增加经销商注册审核的相关接口
}