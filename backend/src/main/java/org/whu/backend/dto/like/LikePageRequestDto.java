package org.whu.backend.dto.like;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.entity.InteractionItemType;

@EqualsAndHashCode(callSuper = true)
@Data
public class LikePageRequestDto extends PageRequestDto {
    @Schema(description = "搜索关键字，例如点赞的项目类型", example = "POST") // 示例值参考实际枚举值
    private InteractionItemType type;
}
