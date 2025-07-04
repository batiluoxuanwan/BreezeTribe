package org.whu.backend.dto.favourite;

import lombok.Builder;
import lombok.Data;
import org.whu.backend.entity.InteractionItemType;

import java.time.LocalDateTime;

@Data
@Builder
// 目前只能收藏旅行团！
public class FavouriteDetailDto {
    private String itemid;
    private InteractionItemType itemType; // 收藏的项目的类型
    private boolean isValid;
    private LocalDateTime createdTime;
}
