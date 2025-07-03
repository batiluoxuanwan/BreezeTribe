package org.whu.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Map<String, String> tokens;
}
