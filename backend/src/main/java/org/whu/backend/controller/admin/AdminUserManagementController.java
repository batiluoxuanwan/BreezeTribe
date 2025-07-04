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
import org.whu.backend.dto.admin.BanRequestDto;
import org.whu.backend.dto.admin.UserManagementDto;
import org.whu.backend.service.admin.AdminService;
import org.whu.backend.util.JpaUtil;

@Tag(name = "管理员-用户管理", description = "管理平台所有类型的账户")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserManagementController {

    @Autowired
    private AdminService adminService;

    @Operation(summary = "获取所有用户/经销商列表（分页）", description = "可通过role,id,name(模糊)筛选角色")
    @GetMapping
    public Result<PageResponseDto<UserManagementDto>> getAllUsers(
            @RequestParam(required = false) String role, // e.g., USER, DEALER
            @RequestParam(required = false) String Id,
            @RequestParam(required = false) String name,
            @Valid @ParameterObject PageRequestDto pageRequestDto) {
        pageRequestDto.setSortBy("createdAt");
        PageResponseDto<UserManagementDto> result = adminService.getAllUsers(pageRequestDto,role,Id,name);
        JpaUtil.notNull(result, "查询失败");
        return Result.success("查询成功", result);
    }

    @Operation(summary = "封禁一个用户或经销商的账户")
    @PostMapping("/{accountId}/ban")
    public Result<?> banAccount(@PathVariable String accountId, @RequestBody BanRequestDto banRequestDto) {
        JpaUtil.isTrue(adminService.banAccount(accountId, banRequestDto), "封禁失败");
        return Result.success("账户已成功封禁");
    }

    @Operation(summary = "解封一个账户")
    @PostMapping("/{accountId}/unban")
    public Result<?> unbanAccount(@PathVariable String accountId) {
        JpaUtil.isTrue(adminService.unbanAccount(accountId), "解封失败");
        return Result.success("账户已成功解封");
    }


}