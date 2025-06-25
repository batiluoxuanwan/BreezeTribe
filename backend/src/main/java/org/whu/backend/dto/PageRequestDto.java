package org.whu.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PageRequestDto {
    @Schema(description = "当前页码，从1开始", example = "1", defaultValue = "1")
    @Min(value = 1, message = "页码必须大于等于1")
    private int page = 1; // 默认第一页

    @Schema(description = "每页记录数，最大100", example = "10", defaultValue = "10")
    @Min(value = 1, message = "每页记录数必须大于等于1")
    @Max(value = 100, message = "每页记录数不能超过100") // 防止一次请求过多数据
    private int size = 10; // 默认每页10条

    @Schema(description = "排序字段，例如：createdTime, title。默认为createdTime", example = "createdTime", defaultValue = "createdTime")
    private String sortBy = "createdTime"; // 默认按创建时间排序

    @Schema(description = "排序方式，ASC (升序) 或 DESC (降序)。默认为DESC", example = "DESC", defaultValue = "DESC", allowableValues = {"ASC", "DESC"})
    @Pattern(regexp = "ASC|DESC", message = "排序方向只能是ASC或DESC")
    private String sortDirection = "DESC"; // 默认降序
}

