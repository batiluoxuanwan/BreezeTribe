package org.whu.backend.dto.admin;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;
import org.whu.backend.entity.accounts.Role;

import java.time.LocalDateTime;

@Data
public class UserManagementDto {
    private String id;
    private String email;
    private String phone;
    private Role role;
    private String username;
    //private String avatarUrl;
    private LocalDateTime banStartTime; // 封禁开始时间
    private int banDurationDays;        // 封禁时长，0=未封禁，-1=永久封禁，正数=天数
    private LocalDateTime createdAt = LocalDateTime.now();
}
