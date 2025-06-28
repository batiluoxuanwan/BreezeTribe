package org.whu.backend.dto.like;

import lombok.Data;
import org.whu.backend.entity.InteractionItemType;

import java.time.LocalDateTime;

@Data
public class LikeDetailDto {
    private String itemid;
    private InteractionItemType itemType; // 点赞的项目的类型
    private LocalDateTime createdTime;
    private String username;
    private String userid;
}
