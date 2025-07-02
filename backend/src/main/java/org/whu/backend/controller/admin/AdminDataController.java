//package org.whu.backend.controller.admin;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.whu.backend.common.Result;
//import org.whu.backend.service.admin.AdminService;
//
//import java.util.Map;
//
//@Tag(name = "管理员-获取统计图表数据", description = "用于获取统计图表数据")
//@RestController
//@RequestMapping("/api/admin/data")
//@PreAuthorize("hasRole('ADMIN')") // 确保只有管理员能访问
//@Slf4j
//public class AdminDataController {
//    @Autowired
//    private AdminService adminService;
//
//    // 1. 用户增长趋势图接口
//    @Operation(summary = "获取用户增长趋势图")
//    @GetMapping("/user-growth")
//    public Result<Map<String, Object>> getUserGrowth(
//            @RequestParam(defaultValue = "month") String period) {
//        // period支持day/week/month
//        Map<String, Object> data = adminService.getUserGrowthData(period);
//        return Result.success(data);
//    }
//
//    // 2. 热门旅游团排行接口
//    @Operation(summary = "获取热门旅游团排行")
//    @GetMapping("/popular-trips")
//    public Result<List<TripRankDTO>> getPopularTrips(
//            @RequestParam(defaultValue = "10") int topN) {
//        List<TripRankDTO> data = adminService.getPopularTrips(topN);
//        return Result.success(data);
//    }
//
//    // 3. 旅行团增长趋势接口
//    @Operation(summary = "获取旅行团增长趋势图")
//    @GetMapping("/trip-growth")
//    public Result<Map<String, Object>> getTripGrowth(
//            @RequestParam(defaultValue = "month") String period) {
//        Map<String, Object> data = adminService.getTripGrowthData(period);
//        return Result.success(data);
//    }
//
//    // 4. 订单数量与收入统计接口
//    @Operation(summary = "获取订单数量与收入流水统计")
//    @GetMapping("/orders-stats")
//    public Result<Map<String, Object>> getOrderStats(
//            @RequestParam(defaultValue = "month") String period) {
//        Map<String, Object> data = adminService.getOrderStats(period);
//        return Result.success(data);
//    }
//
//    // 5. 旅游团平均评分排行接口
//    @Operation(summary = "获取旅游团平均评分排行")
//    @GetMapping("/trip-rating-rank")
//    public Result<List<TripRatingRankDTO>> getTripRatingRank(
//            @RequestParam(defaultValue = "10") int topN) {
//        List<TripRatingRankDTO> data = adminService.getTripRatingRank(topN);
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
//}
