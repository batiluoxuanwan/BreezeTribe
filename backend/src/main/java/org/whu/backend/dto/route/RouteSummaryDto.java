package org.whu.backend.dto.route;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteSummaryDto {
    String id;

    String name;

    String description;

    Integer numOfSpots;
}
