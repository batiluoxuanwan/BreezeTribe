package org.whu.backend.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI内容生成的响应DTO
 */
@Data
public class ContentGenerationResponseDto {
//    @Schema(description = "AI生成的标题")
//    private String title;

    @Schema(description = "AI生成的内容")
    private String content;
}