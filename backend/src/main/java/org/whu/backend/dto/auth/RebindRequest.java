package org.whu.backend.dto.auth;

import lombok.Data;
import org.whu.backend.entity.accounts.Role;

@Data
public class RebindRequest {
    private String newPhone;   // 新手机号
    private String newEmail;   // 新邮箱
    private String code;       // 验证码
    private Role role;
}

