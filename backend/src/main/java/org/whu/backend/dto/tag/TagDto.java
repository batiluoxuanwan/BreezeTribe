package org.whu.backend.dto.tag;

import lombok.Builder;
import lombok.Data;

/**
 * 用于API响应的标签DTO
 */
@Data
@Builder
public class TagDto {
    private String id;
    private String name;
    private String category;
}