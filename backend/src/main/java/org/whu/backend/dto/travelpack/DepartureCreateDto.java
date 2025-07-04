package org.whu.backend.dto.travelpack;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用于批量创建团期的DTO
 */
@Data
public class DepartureCreateDto {
    @NotNull(message = "出发日期不能为空")
    private LocalDateTime departureDate;

    @NotNull
    @DecimalMin(value = "0.00", message = "价格必须大于0")
    @Digits(integer = 10, fraction = 2, message = "价格最多保留两位小数")
    private BigDecimal price;

    @NotNull(message = "容量不能为空")
    @Min(value = 1, message = "容量至少为1")
    private Integer capacity;
}