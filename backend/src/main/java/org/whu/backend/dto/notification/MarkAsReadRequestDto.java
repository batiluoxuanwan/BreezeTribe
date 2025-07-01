package org.whu.backend.dto.notification;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.whu.backend.entity.Notification;

import java.util.List;

@Data
public class MarkAsReadRequestDto {
    @NotEmpty
    private List<Notification.NotificationType> types;
}

