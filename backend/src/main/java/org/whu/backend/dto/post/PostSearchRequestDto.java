package org.whu.backend.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.whu.backend.dto.PageRequestDto;

/**
 * [新增] 用于游记复杂搜索的请求DTO
 */
@EqualsAndHashCode(callSuper = true) // 继承父类的equals和hashCode
@Data
public class PostSearchRequestDto extends PageRequestDto {

    @Schema(description = "模糊搜索关键词，可匹配游记标题、正文、作者名、景点名、城市名等", example = "美食")
    private String keyword;

    @Schema(description = "按指定的标签名进行筛选", example = "美食探店")
    private String tagName;
}