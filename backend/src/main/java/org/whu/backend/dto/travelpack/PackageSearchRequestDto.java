package org.whu.backend.dto.travelpack;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.whu.backend.dto.PageRequestDto;

import java.math.BigDecimal;

@Getter
@Setter
public class PackageSearchRequestDto extends PageRequestDto {

    // --- 搜索条件 ---
    @Schema(description = "模糊搜索关键词，可匹配旅行团标题、描述等", example = "豪华")
    private String keyword;

    @Schema(description = "按城市搜索", example = "巴黎")
    private String city;

    @Schema(description = "最低价格")
    private BigDecimal minPrice;

    @Schema(description = "最高价格")
    private BigDecimal maxPrice;

    @Schema(description = "行程最低天数")
    private Integer minDuration;

    @Schema(description = "行程最高天数")
    private Integer maxDuration;
}