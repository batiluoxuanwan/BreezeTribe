package org.whu.backend.dto.recommend;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RecommendRequestDto {
    @Schema(description = "拿到的候选推荐对象的数量", example = "20", defaultValue = "20")
    @Min(value = 1, message = "推荐池大小必须大于等于1")
    @Max(value = 100, message = "推荐池大小必须小于等于100")
    private int totalNum = 20;

    @Schema(description = "从候选对象中最终选择的数量", example = "5", defaultValue = "5")
    @Min(value = 1, message = "推荐池大小必须大于等于1")
    @Max(value = 100, message = "推荐池大小必须小于等于100")
    private int recommendNum = 5;
}
