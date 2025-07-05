package org.whu.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.accounts.MerchantrankDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.accounts.Role;
import org.whu.backend.service.admin.AdminService;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Tag(name = "管理员-获取统计图表数据", description = "用于获取统计图表数据")
@RestController
@RequestMapping("/api/admin/data")
@Slf4j
public class DataController {
    @Autowired
    private AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')") // 确保只有管理员能访问
    @Operation(summary = "获取用户增长趋势图")
    @GetMapping("/user-growth")
    public Result<Map<String, Object>> getUserGrowth(
            @RequestParam String period, // day/week/month
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    )
    {
        Map<String, Object> data = adminService.getUserGrowthData(period,role,startDate,endDate);
        return Result.success(data);
    }
//

//
    @PreAuthorize("hasRole('ADMIN')") // 确保只有管理员能访问
    @Operation(summary = "获取旅行团增长趋势图")
    @GetMapping("/trip-growth")
    public Result<Map<String, Object>> getTripGrowth(
            @RequestParam String period, // day/week/month
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Map<String, Object> data = adminService.getTravelDepartureGrowth(period, startDate, endDate);
        return Result.success(data);
    }

//

    @PreAuthorize("hasRole('ADMIN')") // 确保只有管理员能访问
    @Operation(summary = "获取订单数量与收入流水统计")
    @GetMapping("/orders-stats")
    public Result<Map<String, Object>> getOrderStats(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> data = adminService.getOrderStats(period, startDate, endDate, merchantId);
        return Result.success(data);
    }

    @Operation(summary = "分页获取旅游团排行榜", description = "支持按浏览量、收藏量、评论量、销售量排序")
    @GetMapping("/trip-rank")
    public Result<PageResponseDto<PackageSummaryDto>> getTripRank(
            @Valid @ParameterObject PageRequestDto pageRequestDto,
            @RequestParam(defaultValue = "COMPREHENSIVE") String rankType) {

        validateSortField(rankType); // 检查排序字段是否合法（防注入）

        PageResponseDto<PackageSummaryDto> resultPage = adminService.getTripRank(pageRequestDto, rankType.toUpperCase());
        return Result.success("团期列表查询成功", resultPage);
    }

    // 仅允许白名单字段参与排序
    private void validateSortField(String sortBy) {
        Set<String> allowedFields = Set.of("VIEW", "FAVORITE", "COMMENT", "SALES", "COMPREHENSIVE","HIDDEN_SCORE", "AVERAGE_RATING");
        if (!allowedFields.contains(sortBy.toUpperCase())) {
            throw new BizException("非法的排行榜排序字段：" + sortBy);
        }
    }
    @Operation(summary = "分页获取商家评分排行榜", description = "支持排序")
    @GetMapping("/merchant-rank")
    public Result<PageResponseDto<MerchantrankDto>> getMerchantRank(
            @Valid @ParameterObject PageRequestDto pageRequestDto) {

        PageResponseDto<MerchantrankDto> resultPage = adminService.getMerchantRank(pageRequestDto);
        return Result.success("团期列表查询成功", resultPage);
    }


//
//// 2. 热门旅游团排行接口
//@Operation(summary = "获取热门旅游团排行")
//@GetMapping("/popular-trips")
//public Result<List<TripRankDTO>> getPopularTrips(
//        @RequestParam(defaultValue = "10") int topN) {
//    List<TripRankDTO> data = adminService.getPopularTrips(topN);
//    return Result.success(data);
//}
//    // 5. 旅游团平均评分排行接口
//    @Operation(summary = "获取旅游团平均评分排行")
//    @GetMapping("/trip-rating-rank")
//    public Result<List<PackageSummaryDto>> getTripRatingRank(
//            @RequestParam(defaultValue = "10") int topN) {
//        List<PackageSummaryDto> data = adminService.getTripRatingRank(topN);
//        return Result.success(data);
//    }
//
//    // 6. 商户平均旅行团评分统计接口
//    @Operation(summary = "获取商户平均旅行团评分")
//    @GetMapping("/merchant-rating-stats")
//    public Result<Map<String, Object>> getMerchantRatingStats() {
//        Map<String, Object> data = adminService.getMerchantRatingStats();
//        return Result.success(data);
//    }
}
