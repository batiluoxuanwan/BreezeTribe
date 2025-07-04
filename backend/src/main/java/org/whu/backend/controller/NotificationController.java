package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.notification.MarkAsReadRequestDto;
import org.whu.backend.dto.notification.NotificationDto;
import org.whu.backend.dto.notification.UnreadCountDto;
import org.whu.backend.service.NotificationService;
import org.whu.backend.util.AccountUtil;

@Tag(name = "用户/经销商-通知中心", description = "用户/经销商获取和管理自己的通知")
@RestController
@PreAuthorize("hasRole('USER') or hasRole('MERCHANT')")
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 获取我的通知列表，现在支持按类别筛选
    @Operation(summary = "获取我的通知列表（分页，可按类别筛选）")
    @GetMapping
    public Result<PageResponseDto<NotificationDto>> getMyNotifications(
            @Parameter(description = "通知类别: likes (点赞与收藏), comments (评论与回复), system (系统与订单)。留空则获取所有。")
            @RequestParam(required = false) String category,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问获取通知列表接口, 类别: {}", currentUserId, category);
        PageResponseDto<NotificationDto> resultPage = notificationService.getMyNotifications(currentUserId, category, pageRequestDto);
        return Result.success(resultPage);
    }

    @Operation(summary = "获取我的未读通知数量（按类型分组）")
    @GetMapping("/unread-counts")
    public Result<UnreadCountDto> getUnreadCount() {
        String currentUserId = AccountUtil.getCurrentAccountId();
        return Result.success(notificationService.getUnreadCounts(currentUserId));
    }

    @Operation(summary = "将指定类型的通知标记为已读")
    @PostMapping("/mark-as-read")
    public Result<?> markAsRead(
            @Parameter(description = "通知类别: likes (点赞与收藏), comments (评论与回复), system (系统与订单)。留空则标记所有为已读。")
            @Valid @RequestBody MarkAsReadRequestDto request) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        notificationService.markAsRead(currentUserId, request);
        return Result.success("操作成功");
    }
}