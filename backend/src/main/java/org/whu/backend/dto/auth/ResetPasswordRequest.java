package org.whu.backend.dto.auth;

import lombok.Data;
import org.whu.backend.entity.accounts.Role;

@Data
public class ResetPasswordRequest {
    private String email;
    private String phone;
    private String newpassword;
    private String code;
    private Role role;
}
