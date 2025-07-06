package org.whu.backend.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.whu.backend.dto.accounts.AuthorDto;
import org.whu.backend.entity.Report;

import java.time.LocalDateTime;

/**
 * 管理员查看举报列表的DTO
 */
@Data
@Builder
public class ReportDto {
    @Schema(description = "举报记录ID")
    private String id;
    
    @Schema(description = "被举报内容ID")
    private String reportedItemId;
    
    @Schema(description = "被举报内容类型")
    private Report.ReportedItemType itemType;
    
    @Schema(description = "举报原因")
    private Report.ReportReason reason;
    
    @Schema(description = "附加信息")
    private String additionalInfo;

    @Schema(description = "举报的内容摘要（主要用于评论）")
    private String summary;
    
    @Schema(description = "举报状态")
    private Report.ReportStatus status;
    
    @Schema(description = "举报人信息")
    private AuthorDto reporter;
    
    @Schema(description = "举报创建时间")
    private LocalDateTime createdTime;
}