package org.whu.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BanRequestDto {
    @Schema(description = "封禁原因")
    @NotBlank
    private String reason;

    @Schema(description = "封禁时长（天），-1表示永久封禁")
    private int durationInDays = -1;
}