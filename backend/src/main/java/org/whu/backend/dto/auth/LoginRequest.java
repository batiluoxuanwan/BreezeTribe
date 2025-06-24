package org.whu.backend.dto.auth;

import lombok.Data;
import org.whu.backend.entity.Role;

@Data
public class LoginRequest {
    private String identity;
    private String password;
    private Role role;
}
