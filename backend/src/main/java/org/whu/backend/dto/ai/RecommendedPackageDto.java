package org.whu.backend.dto.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendedPackageDto {
    private String id;
    private String reason;
}