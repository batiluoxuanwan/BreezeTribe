package org.whu.backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.whu.backend.service.ai.AiModerationService;

@Data
@AllArgsConstructor
public class ModerationDetails {
    private AiModerationService.ModerationResult suggestion; // 最终的建议 (PASS, REVIEW, BLOCK)
    private String reason;               // AI返回的详细原因JSON字符串
}