package org.whu.backend.dto.travelpack;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PackageUpdateRequestDto {
    @Schema(description = "新的旅行团标题")
    @NotBlank
    private String title;

    @Schema(description = "新的图文详情")
    private String detailedDescription;

}