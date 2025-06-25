package org.whu.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.whu.backend.entity.Favorite;

@Data
public class FavoritePageReqDto extends PageRequestDto {
    @Schema(description = "搜索关键字，例如收藏类型", example = "SCENIC_SPOT") // 示例值参考实际枚举值
    private Favorite.FavoriteItemType type;
}
