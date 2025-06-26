package org.whu.backend.controller;

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
import org.whu.backend.dto.spot.SpotCreateRequestDto;
import org.whu.backend.dto.spot.SpotSummaryDto;
import org.whu.backend.dto.spot.SpotUpdateRequestDto;
import org.whu.backend.entity.Spot;
import org.whu.backend.service.AdminService;
import org.whu.backend.util.JpaUtil;

@Tag(name = "管理员-景点管理", description = "管理员维护平台景点缓存数据库")
@RestController
@RequestMapping("/api/admin/spots")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSpotController {
    @Autowired
    private AdminService adminService;

    @Operation(summary = "手动创建一个新景点")
    @PostMapping
    public Result<SpotSummaryDto> createSpot(@RequestBody SpotCreateRequestDto spotCreateRequestDto) {
        SpotSummaryDto result = adminService.createSpot(spotCreateRequestDto);
        JpaUtil.notNull(result, "创建失败");
        return Result.success("创建完毕",result);
    }

    @Operation(summary = "更新一个景点的信息")
    @PutMapping("/{id}")
    public Result<SpotSummaryDto> updateSpot(@PathVariable String id, @RequestBody SpotUpdateRequestDto spotUpdateRequestDto) {
        SpotSummaryDto result = adminService.updateSpot(id, spotUpdateRequestDto);
        JpaUtil.notNull(result, "更新失败");
        return Result.success();
    }

    @Operation(summary = "删除一个缓存的景点")
    @DeleteMapping("/{id}")
    public Result<?> deleteSpot(@PathVariable String id) {
        JpaUtil.isTrue(adminService.deleteSpot(id), "删除失败");
        return Result.success();
    }

    @Operation(summary = "分页查询数据库中已存在的景点")
    @GetMapping
    public Result<PageResponseDto<SpotSummaryDto>> getSpots(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        PageResponseDto<SpotSummaryDto> result = adminService.getSpots(pageRequestDto);
        JpaUtil.notNull(result, "查询失败");
        return Result.success("查询完毕",result);
    }
}