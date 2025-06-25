package org.whu.backend.dto.travelpack;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 旅行团摘要信息 DTO (用于列表展示)
 */
@Data
public class PackageSummaryDto {

    @Schema(description = "旅行团id")
    private String id;

    @Schema(description = "旅行团标题")
    private String title;

    @Schema(description = "详细描述")
    private String description;

    @Schema(description = "旅行团封面带签名的URL")
    private String coverImageUrl;


    @Schema(description = "旅行团价格")
    private BigDecimal price;

    @Schema(description = "旅行团持续天数")
    private Integer durationInDays;

    @Schema(description = "旅行团目前的状态")
    private String status;
}