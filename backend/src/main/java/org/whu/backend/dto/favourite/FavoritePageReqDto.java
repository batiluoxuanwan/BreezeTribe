package org.whu.backend.dto.favourite;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.entity.InteractionItemType;

@EqualsAndHashCode(callSuper = true)
@Data
public class FavoritePageReqDto extends PageRequestDto {
    @Schema(description = "搜索关键字，例如收藏类型", example = "SCENIC_SPOT") // 示例值参考实际枚举值
    private InteractionItemType type;
}
