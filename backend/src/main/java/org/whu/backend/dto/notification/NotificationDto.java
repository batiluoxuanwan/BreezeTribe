package org.whu.backend.dto.notification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDto {
    private String id;
    private boolean isRead;
    private String type;
    private String description;
    private String content;
    private String triggerUserId;
    private String triggerUsername;
    private String triggerUserAvatarUrl;
    private String relatedItemId;
    private LocalDateTime createdTime;
}
