package org.whu.backend.dto.like;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.whu.backend.entity.Like;

@Data
public class LikeRequestDto {
    @Schema(description = "要收藏/取消收藏的项目ID")
    @NotBlank
    private String itemId;

    @Schema(description = "项目类型，目前只支持POST点赞", example = "POST")
    @NotNull
    private Like.LikeItemType itemType;
}
