package org.whu.backend.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PostUpdateRequestDto {
    @Schema(description = "新的游记标题")
    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    @Schema(description = "新的游记正文内容")
    @NotBlank(message = "内容不能为空")
    private String content;

    @Schema(description = "新的关联景点ID (可选, 传入空字符串或null表示不关联)")
    private String spotId;

    @Schema(description = "更新后的图片MediaFile ID列表")
    @Size(min = 1, message = "至少需要包含一张图片")
    private List<String> imageIds;
}