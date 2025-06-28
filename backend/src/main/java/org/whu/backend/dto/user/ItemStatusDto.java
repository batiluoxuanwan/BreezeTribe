package org.whu.backend.dto.user;

import lombok.Builder;
import lombok.Data;

/**
 *  单个项目的互动状态
 */
@Data
@Builder
public class ItemStatusDto {
    private boolean isLiked;
    private boolean isFavorited;
}