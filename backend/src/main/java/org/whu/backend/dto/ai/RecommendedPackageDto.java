package org.whu.backend.dto.ai;

import lombok.Builder;
import lombok.Data;
import org.whu.backend.dto.travelpack.PackageSummaryDto;

@Data
@Builder
public class RecommendedPackageDto {
    private PackageSummaryDto packageSummaryDto;
    private String reason;
}