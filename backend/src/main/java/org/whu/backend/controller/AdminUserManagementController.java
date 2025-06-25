package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.admin.BanRequestDto;
import org.whu.backend.dto.admin.UserManagementDto;

@Tag(name = "管理员-用户管理", description = "管理平台所有类型的账户")
@RestController
@RequestMapping("/api/admin/users")
// @PreAuthorize("hasRole('ADMIN')")
public class AdminUserManagementController {

    @Operation(summary = "获取所有用户/经销商列表（分页）", description = "可通过参数筛选角色类型")
    @GetMapping
    public Result<PageResponseDto<UserManagementDto>> getAllUsers(
            @RequestParam(required = false) String role, // e.g., USER, DEALER
            @Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO: 分页查询账户信息
        return Result.success();
    }

    @Operation(summary = "封禁一个用户或经销商的账户")
    @PostMapping("/{accountId}/ban")
    public Result<?> banAccount(@PathVariable String accountId, @RequestBody BanRequestDto banRequestDto) {
        // TODO: 将账户状态设置为 BANNED，并记录封禁原因和时长
        return Result.success("账户已成功封禁");
    }

    @Operation(summary = "解封一个账户")
    @PostMapping("/{accountId}/unban")
    public Result<?> unbanAccount(@PathVariable String accountId) {
        // TODO: 将账户状态恢复为 ACTIVE
        return Result.success("账户已成功解封");
    }
}