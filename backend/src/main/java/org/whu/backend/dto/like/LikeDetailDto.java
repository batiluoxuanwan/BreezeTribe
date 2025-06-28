package org.whu.backend.dto.like;

import lombok.Data;
import org.whu.backend.entity.Like;

import java.time.LocalDateTime;

@Data
public class LikeDetailDto {
    private String itemid;
    private Like.LikeItemType itemType; // 点赞的项目的类型
    private LocalDateTime createdTime;
    private String username;
    private String userid;
}
