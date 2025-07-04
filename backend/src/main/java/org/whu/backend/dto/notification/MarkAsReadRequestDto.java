package org.whu.backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class MarkAsReadRequestDto {
    @NotBlank(message = "通知类别不能为空")
    private String category; // "likes", "comments", or "system"
}

