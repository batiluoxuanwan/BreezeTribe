package org.whu.backend.dto.auth;

import lombok.Data;
import org.whu.backend.entity.accounts.Role;

@Data
public class RegisterRequest {
    private String email;
    private String phone;
    private String password;
    private String code;
    private Role role;
}
