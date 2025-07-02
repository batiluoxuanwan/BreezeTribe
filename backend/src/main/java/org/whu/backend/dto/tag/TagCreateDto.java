package org.whu.backend.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.whu.backend.entity.Tag;

@Data
public class TagCreateDto {
    @NotBlank(message = "标签名称不能为空")
    private String name;

    @NotNull(message = "必须为标签指定一个分类")
    private Tag.TagCategory category;
}
