package org.whu.backend.dto.accounts;

import lombok.Builder;
import lombok.Data;

// 展示一个人的简略信息用的
@Data
@Builder
public class AuthorDto {
    private String id;
    private String username;
    private String avatarUrl;
}