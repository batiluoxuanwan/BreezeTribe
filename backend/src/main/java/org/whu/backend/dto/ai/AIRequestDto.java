package org.whu.backend.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * [新增] 发送给AI模型的请求体DTO
 */
@Data
@Builder
public class AIRequestDto {
    private String model;
    private List<MessageDto> messages;
    private Double temperature;
    private Boolean stream;
}