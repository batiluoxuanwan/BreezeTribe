package org.whu.backend.dto.tag;

import lombok.Data;
import org.whu.backend.entity.Tag;

/**
 * 用于更新标签的DTO
 */
@Data
public class TagUpdateDto {
    private String name;
    private Tag.TagCategory category;
}