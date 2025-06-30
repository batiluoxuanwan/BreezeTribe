package org.whu.backend.dto.accounts;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 *  用于展示用户公开主页信息的DTO
 */
@Data
@Builder
public class UserProfileDto {

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户头像URL (带签名)")
    private String avatarUrl;

    // TODO: 未来可以增加“关注数”、“粉丝数”、“获赞数”等统计字段
}