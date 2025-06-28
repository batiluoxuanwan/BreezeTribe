package org.whu.backend.dto.favourite;

import lombok.Data;
import org.whu.backend.entity.InteractionItemType;

import java.time.LocalDateTime;

@Data
public class FavouriteDetailDto {
    private String itemid;
    private InteractionItemType itemType; // 收藏的项目的类型
    private LocalDateTime createdTime;
    private String username;
    private String userid;
}
