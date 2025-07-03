package org.whu.backend.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TravelOrderDetailDto {
    private String id;
    private String departureId;
    private String packageId;
    private String packageTitle;
    private String packageCoverImageUrl;
    private Integer travelerCount;
    private BigDecimal totalPrice;
    private String status;

    private String contactName;
    private String contactPhone;

    private String userId;
    private LocalDateTime departureTime;
    private LocalDateTime orderTime;
}
