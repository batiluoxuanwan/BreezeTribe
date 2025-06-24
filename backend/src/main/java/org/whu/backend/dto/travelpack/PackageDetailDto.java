package org.whu.backend.dto.travelpack;

import lombok.Data;
import org.whu.backend.dto.route.RouteDetailDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 旅行团详情 DTO (用于详情页展示)
 * 它包含了更丰富的信息，包括嵌套的路线和景点信息。
 */
@Data
public class PackageDetailDto {
    // 包含摘要信息的所有字段
    private String id;
    private String title;
    private String coverImageUrl;
    private BigDecimal price;
    private Integer durationInDays;
    private String detailedDescription;

    // 嵌套的路线信息
    private List<RouteDetailDto> routes;
}