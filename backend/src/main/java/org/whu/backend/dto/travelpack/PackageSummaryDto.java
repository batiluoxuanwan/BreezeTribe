package org.whu.backend.dto.travelpack;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;


/**
 * 旅行团摘要信息 DTO (用于列表展示)
 */
@Data
@Builder
public class PackageSummaryDto {

    @Schema(description = "旅行团id")
    private String id;

    @Schema(description = "旅行团标题")
    private String title;

    @Schema(description = "详细描述")
    private String description;

    @Schema(description = "旅行团封面带签名的URL")
    private String coverImageUrl;


    @Schema(description = "旅行团起步价格价格")
    private BigDecimal price;

    @Schema(description = "旅行团持续天数")
    private Integer durationInDays;

    @Schema(description = "旅行团的收藏数量")
    private Integer favouriteCount;

    @Schema(description = "旅行团的评论数量")
    private Integer commentCount;

    @Schema(description = "旅行团的浏览量")
    private Integer viewCount;

    @Schema(description = "旅行团目前的状态")
    private String status;



}