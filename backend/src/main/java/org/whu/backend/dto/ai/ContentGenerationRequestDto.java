package org.whu.backend.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI内容生成的请求DTO
 */
@Data
public class ContentGenerationRequestDto {
    @Schema(description = "要生成内容的核心文本或关键词", example = "目的地：张家界，特色：玻璃栈道，自然风光，3日游")
    @NotBlank(message = "核心内容不能为空")
    private String text;

    @Schema(description = "请求者的角色，用于AI切换不同的人设和文风", example = "MERCHANT")
    @NotNull(message = "角色不能为空")
    private RequestRole role;

    public enum RequestRole {
        MERCHANT, // 商家，需要营销推广文案
        USER      // 用户，需要生动的个人游记风格
    }
}