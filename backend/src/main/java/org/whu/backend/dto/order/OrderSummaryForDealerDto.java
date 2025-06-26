package org.whu.backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderSummaryForDealerDto {

    @Schema(description = "订单ID")
    private String orderId;

    @Schema(description = "出行人数")
    private Integer travelerCount;

    @Schema(description = "订单总价")
    private BigDecimal totalPrice;

    @Schema(description = "联系人姓名（已脱敏）", example = "张**")
    private String contactName;

    @Schema(description = "联系人电话（已脱敏）", example = "138****8000")
    private String contactPhone;

    @Schema(description = "下单时间")
    private LocalDateTime orderTime;
}