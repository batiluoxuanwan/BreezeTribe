package org.whu.backend.dto.accounts;

import jakarta.persistence.*;
import lombok.Data;
import org.whu.backend.entity.accounts.Role;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ShareDto {
    private String id;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String username;

    private String avatarUrl;

    private boolean active = false;
}
