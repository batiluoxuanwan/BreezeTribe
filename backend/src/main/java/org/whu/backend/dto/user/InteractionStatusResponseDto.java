package org.whu.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 *  批量查询互动状态的响应体
 * 使用Map结构，方便前端根据 "type_id" 快速查找
 */
@Data
@Builder
public class InteractionStatusResponseDto {
    @Schema(description = "项目的互动状态映射。Key格式为 'TYPE_ID', 例如 'POST_uuid123'")
    private Map<String, ItemStatusDto> statusMap;
}