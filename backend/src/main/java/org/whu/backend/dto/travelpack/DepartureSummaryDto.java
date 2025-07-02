package org.whu.backend.dto.travelpack;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DepartureSummaryDto {
    private String id;
    private LocalDateTime departureDate;
    private BigDecimal price;
    private Integer capacity;
    private Integer participants;
    private String status;
    private String packageId;
    private String packageTitle;
}
