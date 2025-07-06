package org.whu.backend.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.whu.backend.entity.Report;

/**
 * 用户提交举报的请求DTO
 */
@Data
public class ReportCreateDto {
    @NotBlank
    @Schema(description = "举报的项目id")
    private String reportedItemId;

    @NotNull
    @Schema(description = "举报的项目类型")
    private Report.ReportedItemType itemType;

    @NotNull
    @Schema(description = "举报的原因，SPAM_AD(垃圾广告), PORNOGRAPHIC(色情内容), ILLEGAL_CONTENT(违法内容), PERSONAL_ATTACK(人身攻击), OTHER(其他原因)")
    private Report.ReportReason reason;

    @Schema(description = "用户附加的描述信息")
    private String additionalInfo;
}