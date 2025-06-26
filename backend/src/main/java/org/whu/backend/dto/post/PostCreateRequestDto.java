package org.whu.backend.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

// 创建游记的请求DTO
@Data
public class PostCreateRequestDto {

    @Schema(description = "游记标题")
    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    @Schema(description = "游记正文内容")
    @NotBlank(message = "内容不能为空")
    private String content;

    @Schema(description = "关联的景点ID (可选)")
    private String spotId;

    @Schema(description = "游记的图片MediaFile ID列表，按顺序排列，第一张为封面")
    @NotEmpty(message = "至少需要上传一张图片")
    private List<String> imageIds;

    // TODO: 未来可以增加标签等字段
    // private List<String> tags;
}