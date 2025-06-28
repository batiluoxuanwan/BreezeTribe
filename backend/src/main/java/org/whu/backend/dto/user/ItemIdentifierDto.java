package org.whu.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.whu.backend.entity.InteractionItemType;

/**
 *  用于标识一个具体项目的DTO
 */
@Data
public class ItemIdentifierDto {
    @Schema(description = "项目ID")
    @NotBlank
    private String id;

    @Schema(description = "项目类型 (PACKAGE, SPOT, ROUTE, POST)")
    @NotNull
    private InteractionItemType type;
}

