package org.whu.backend.dto.favourite;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.whu.backend.entity.InteractionItemType;

@Data
public class FavoriteRequestDto {
    @Schema(description = "要收藏/取消收藏的项目ID")
    @NotBlank
    private String itemId;

    @Schema(description = "项目类型 (PACKAGE, SPOT, ROUTE)", example = "PACKAGE")
    @NotNull
    private InteractionItemType itemType;
}
