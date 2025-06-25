package org.whu.backend.dto.route;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RouteCreateRequestDto {
    @Schema(description = "路线名称", example = "巴黎艺术一日游")
    @NotBlank(message = "路线名称不能为空")
    private String name;

    @Schema(description = "路线描述", example = "探索卢浮宫的奥秘，漫步在塞纳河畔。")
    private String description;

    @Schema(description = "组成路线的景点的百度地图UID列表")
    @Size(min = 1, message = "至少需要包含一个景点")
    private List<String> spotUids;
}