package org.whu.backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderCreateRequestDto {
    @Schema(description = "要报名的旅行团ID")
    @NotBlank
    private String packageId;

    @Schema(description = "出行人数", example = "2")
    @Min(1)
    private int travelerCount;

    @Schema(description = "联系人姓名", example = "张三")
    @NotBlank
    private String contactName;

    @Schema(description = "联系人电话", example = "13800138000")
    @NotBlank
    private String contactPhone;
}
