package org.whu.backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * [新增] 用于在“我的评价”页面展示的订单信息DTO
 */
@Data
@Builder
public class OrderForReviewDto {

    @Schema(description = "订单ID")
    private String orderId;

    @Schema(description = "团期ID")
    private String departureId;

    @Schema(description = "旅行团ID")
    private String packageId;

    @Schema(description = "旅行团标题")
    private String packageTitle;

    @Schema(description = "旅行团封面图URL (带签名)")
    private String packageCoverImageUrl;

    @Schema(description = "下单时间")
    private LocalDateTime orderTime;

    @Schema(description = "出发时间")
    private LocalDateTime departureDate;

    @Schema(description = "评价的评分")
    private Integer star;

    @Schema(description = "评价的内容")
    private String content;

//    @Schema(description = "完成时间")
//    private LocalDateTime completedTime; // 订单完成时间

    @Schema(description = "是否已经评价")
    private boolean hasReviewed;

    @Schema(description = "订单总价")
    private String totalPrice;
}