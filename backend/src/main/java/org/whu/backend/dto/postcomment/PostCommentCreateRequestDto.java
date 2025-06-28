package org.whu.backend.dto.postcomment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostCommentCreateRequestDto {
    @Schema(description = "评论内容")
    @NotBlank
    private String content;

    @Schema(description = "要回复的父评论ID (如果是对游记的直接评论，则此字段为null或空字符串)")
    private String parentId;
}