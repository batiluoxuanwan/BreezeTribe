package org.whu.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [新增] 消息内容的嵌套DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private String role;
    private String content;
}
