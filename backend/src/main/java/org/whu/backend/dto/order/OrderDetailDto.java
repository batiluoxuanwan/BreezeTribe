package org.whu.backend.dto.order;

import lombok.Data;
import org.whu.backend.entity.Order;
import org.whu.backend.entity.accounts.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDetailDto {
    private Integer travelerCount;
    private BigDecimal totalPrice;
    private Order.OrderStatus status; // 订单状态
    private LocalDateTime createdTime;
    private String username;
    private String travelPackageTitle;
    private String OrderId;
}
