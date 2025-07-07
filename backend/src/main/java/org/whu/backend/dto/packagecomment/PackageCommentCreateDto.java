package org.whu.backend.dto.packagecomment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PackageCommentCreateDto {
    @Schema(description = "评价星级 (1-5)，如果是回复就随便给一个星级")
    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    @Schema(description = "评价内容")
    @NotBlank
    private String content;

    @Schema(description = "要回复的父评论ID (如果是对旅行团的直接评价，则此字段为null)")
    private String parentId;
}