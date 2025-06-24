package org.whu.backend.dto.travelpack;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 旅行团摘要信息 DTO (用于列表展示)
 */
@Data
public class PackageSummaryDto {
    private String id;
    private String title;
    private String coverImageUrl;
    private BigDecimal price;
    private Integer durationInDays;
}