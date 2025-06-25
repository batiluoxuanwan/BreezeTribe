package org.whu.backend.dto.route;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RouteUpdateRequestDto {
    @Schema(description = "新的路线名称", example = "巴黎浪漫风情一日游")
    @NotBlank(message = "路线名称不能为空")
    private String name;

    @Schema(description = "新的路线描述", example = "在塞纳河畔体验法式浪漫。")
    private String description;

    @Schema(description = "更新后的景点百度地图UID列表")
    @Size(min = 1, message = "至少需要包含一个景点")
    private List<String> spotUids;
}