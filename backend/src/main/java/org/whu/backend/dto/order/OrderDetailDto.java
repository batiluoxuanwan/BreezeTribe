package org.whu.backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *  用于展示用户个人订单详情的DTO
 */
@Data
@Builder
public class OrderDetailDto {

    @Schema(description = "订单ID")
    private String orderId;

    @Schema(description = "旅行团ID")
    private String packageId;

    @Schema(description = "旅行团标题")
    private String packageTitle;

    @Schema(description = "旅行团封面图URL")
    private String packageCoverImageUrl;

    @Schema(description = "出行人数")
    private Integer travelerCount;

    @Schema(description = "订单总价")
    private BigDecimal totalPrice;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "下单时间")
    private LocalDateTime orderTime;
}