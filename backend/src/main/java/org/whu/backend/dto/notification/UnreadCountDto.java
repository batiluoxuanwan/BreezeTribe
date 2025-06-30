package org.whu.backend.dto.notification;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class UnreadCountDto {
    private long totalUnread;
    private Map<String, Long> countsByType;
}