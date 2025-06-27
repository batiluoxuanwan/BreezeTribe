package org.whu.backend.dto.travelpack;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.whu.backend.dto.route.RouteDetailDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 旅行团详情 DTO (用于详情页展示)
 * 它包含了更丰富的信息，包括嵌套的路线和景点信息。
 */
@Data
@Builder
public class PackageDetailDto {
    // 包含摘要信息的所有字段
    @Schema(description = "旅行团id")
    private String id;

    @Schema(description = "旅行团标题")
    private String title;

    @Schema(description = "旅行团图集")
    private List<String> coverImageUrls;

    @Schema(description = "旅行团价格")
    private BigDecimal price;

    @Schema(description = "旅行团持续天数")
    private Integer durationInDays;

    @Schema(description = "旅行团描述")
    private String detailedDescription;

    @Schema(description = "旅行团收藏量")
    private Integer favouriteCount;

    @Schema(description = "旅行团状态")
    private String status;

    // 嵌套的路线信息
    @Schema(description = "旅行团路线信息")
    private List<RouteDetailDto> routes;
}