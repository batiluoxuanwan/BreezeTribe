package org.whu.backend.dto.travelpack;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * 【新增】给经销商看的旅行团详情DTO
 * 它继承自公共的详情DTO，并增加了团期列表信息。
 */
@Data
@EqualsAndHashCode(callSuper = true) // 确保equals和hashCode能正确处理父类字段
@SuperBuilder // 子类同样使用 @SuperBuilder
public class PackageDetailForMerchantDto extends PackageDetailDto {

    @Schema(description = "该产品下的所有团期列表（无论状态）")
    private List<DepartureSummaryDto> departures;

    @Schema(description = "图片id于imageUrl的映射列表")
    private Map<String, String> imageIdAndUrls; // 返回带签名的图片Id与URL列表
}