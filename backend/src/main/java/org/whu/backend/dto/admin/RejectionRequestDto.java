package org.whu.backend.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectionRequestDto {
    @Schema(description = "驳回原因")
    @NotBlank
    private String reason;
}
