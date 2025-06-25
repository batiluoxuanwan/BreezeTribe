package org.whu.backend.dto.route;

import lombok.Builder;
import lombok.Data;
import org.whu.backend.dto.spot.SpotDetailDto;

import java.util.List;

/**
 * 路线信息 DTO (用于嵌套在PackageDetailDto中)
 */
@Data
@Builder
public class RouteDetailDto {
    private String id;
    private String name;
    private String description;

    // 嵌套的景点信息
    private List<SpotDetailDto> spots;
}