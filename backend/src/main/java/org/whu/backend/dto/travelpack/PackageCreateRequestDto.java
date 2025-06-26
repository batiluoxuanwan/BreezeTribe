package org.whu.backend.dto.travelpack;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建旅行团的请求DTO
 */
@Data
public class PackageCreateRequestDto {

    @Schema(description = "旅行团标题")
    @NotBlank
    private String title;

    @Schema(description = "详情描述")
    private String detailedDescription;

    @Schema(description = "价格")
    @NotNull
    @DecimalMin(value = "0.00", message = "价格不能为负数")
    @Digits(integer = 10, fraction = 2, message = "价格最多保留两位小数")
    private BigDecimal price;

    @Schema(description = "总容量")
    @NotNull
    private Integer capacity;

    @Schema(description = "出发日期")
    private LocalDateTime departureDate;

    @Schema(description = "行程天数")
    private Integer durationInDays;

    @Schema(description = "组成旅行团的路线列表，按照顺序给")
    @NotEmpty
    private List<String> routesIds;

    @Schema(description = "组成旅行团的图片集，按照顺序给，第一个默认是封面")
    @NotEmpty
    private List<String> imgIds;
}
