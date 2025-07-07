package org.whu.backend.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.whu.backend.dto.report.ModerationDetails;
import org.whu.backend.entity.Report;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.repository.ReportRepository;
import org.whu.backend.repository.authRepo.AuthRepository;
import org.whu.backend.service.admin.AdminContentService;
import org.whu.backend.service.ai.AiModerationService;
import org.whu.backend.util.JpaUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI内容审核事件监听器
 */
@Component
@Slf4j
public class AiModerationListener {

    @Autowired
    private AiModerationService aiModerationService;
    @Autowired
    private AdminContentService adminContentService; // 注入我们已有的管理员服务
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ObjectMapper objectMapper; // 注入JSON处理工具
    @Autowired
    private AuthRepository authRepository;

    /**
     * 监听内容提交事件，并异步执行审核
     * @param event 包含内容信息的事件对象
     */
    @Async // 使用@Async，让审核在后台进行，不影响用户体验！
    @EventListener
    public void handleContentSubmission(ContentSubmissionEvent event) {
        log.info("监听到内容提交事件：类型[{}], ID[{}]，准备进行AI审核...", event.getItemType(), event.getItemId());

        try {
            // 1. 调用AI服务进行审核，获取包含建议和原因的详细结果
            ModerationDetails details = aiModerationService.moderateText(event.getTextContent());

            // 2. 如果AI判断为不合规(BLOCK)，则调用管理员服务进行删除
            if (details.getSuggestion() == AiModerationService.ModerationResult.BLOCK) {
                createReportFromAi(event, details.getReason());
                log.warn("内容ID [{}] 被AI判定为[BLOCK]，将调用管理员服务进行删除！原因: {}", event.getItemId(), details.getReason());
//                switch (event.getItemType()) {
//                    case TRAVEL_POST -> adminContentService.deleteTravelPost(event.getItemId());
//                    case POST_COMMENT -> adminContentService.deletePostComment(event.getItemId());
//                    case PACKAGE_COMMENT -> adminContentService.deletePackageComment(event.getItemId());
//                }
//                log.info("已成功删除不合规内容，ID [{}]。", event.getItemId());
            }

            // 3. 如果结果是 REVIEW，可以更新内容的数据库状态为“待人工审核”
            else if (details.getSuggestion() == AiModerationService.ModerationResult.REVIEW) {
                log.warn("内容ID [{}] 被AI判定为[REVIEW]，需要人工进一步审核。原因: {}", event.getItemId(), details.getReason());
                createReportFromAi(event, details.getReason());
            }

        } catch (Exception e) {
            // 即使审核或删除过程中发生异常，我们也要捕获它，防止异步线程崩溃
            log.error("处理内容审核事件时发生未知异常！内容ID: {}", event.getItemId(), e);
        }
    }

    /**
     * 私有辅助方法：由AI自动创建一条举报记录
     */
    private void createReportFromAi(ContentSubmissionEvent event, String aiReason) {
        Report report = new Report();
        // Account ai = JpaUtil.getOrThrow(authRepository,"11111111-1111-1111-1111-11111111","找不到AI账户");
        report.setReporter(null); // 如果是null，举报人就是AI
        report.setReportedItemId(event.getItemId());
        report.setItemType(mapToReportedItemType(event.getItemType())); // 类型转换
        report.setReason(Report.ReportReason.OTHER); // 举报原因设为“其他”
        report.setAdditionalInfo("AI自动提审，建议人工复核。AI分析原因：" + parseAndFormatAiReason(aiReason)); // 将AI的原因写入补充信息
        reportRepository.save(report);
        log.info("已成功为内容ID [{}] 创建AI举报单。", event.getItemId());
    }

    /**
     * 私有辅助方法：将审核类型映射到举报类型
     */
    private Report.ReportedItemType mapToReportedItemType(ContentSubmissionEvent.ModerationItemType itemType) {
        return switch (itemType) {
            case TRAVEL_POST -> Report.ReportedItemType.TRAVEL_POST;
            case POST_COMMENT -> Report.ReportedItemType.POST_COMMENT;
            case PACKAGE_COMMENT -> Report.ReportedItemType.PACKAGE_COMMENT;
        };
    }

    /**
     * 私有辅助方法：解析并格式化AI返回的Reason JSON
     * 现在它可以同时处理JSON数组和单个JSON对象。
     */
    private String parseAndFormatAiReason(String jsonReason) {
        if (!StringUtils.hasText(jsonReason)) {
            return "无具体原因。";
        }

        List<Map<String, Object>> reasons;
        try {
            // 优先尝试解析为对象列表 (e.g., [{}, {}])
            reasons = objectMapper.readValue(jsonReason, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            // 如果失败，再尝试解析为单个对象 (e.g., {})
            try {
                Map<String, Object> singleReason = objectMapper.readValue(jsonReason, new TypeReference<>() {});
                reasons = Collections.singletonList(singleReason); // 把它包装成一个单元素的列表
            } catch (JsonProcessingException e2) {
                log.error("解析AI Reason JSON失败，既不是数组也不是对象。JSON: {}", jsonReason, e2);
                return "AI返回原因解析失败，原始JSON: " + jsonReason;
            }
        }

        if (reasons.isEmpty()) {
            return "无具体原因。";
        }

        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> reason : reasons) {
            String riskLevel = (String) reason.getOrDefault("riskLevel", "未知");
            String riskTips = (String) reason.getOrDefault("riskTips", "无");
            String riskWords = (String) reason.getOrDefault("riskWords", "无");
            sb.append(String.format("\n- 风险等级: %s, 风险提示: %s, 风险词: %s", riskLevel, riskTips, riskWords));
        }
        return sb.toString();
    }
}