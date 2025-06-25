package org.whu.backend.dto.auth;

import lombok.Data;
import org.whu.backend.entity.Accounts.Role;

@Data
public class LoginRequest {
    private String identity;
    private String password;
    private Role role;
}
