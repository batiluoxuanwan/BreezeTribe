package org.whu.backend.dto.travelpack;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

// [新增] 用于描述单日行程的嵌套DTO
@Data
public class DayScheduleDto {
    @Schema(description = "这是第几天的行程", example = "1")
    @NotNull
    @Min(1)
    private Integer dayNumber;

    @Schema(description = "当天路线的名称", example = "巴黎市区经典一日游")
    @NotBlank
    private String routeName;

    @Schema(description = "当天路线的描述")
    private String routeDescription;

    @Schema(description = "组成当天路线的景点的百度地图UID列表")
    @NotEmpty
    private List<String> spotUids;
}