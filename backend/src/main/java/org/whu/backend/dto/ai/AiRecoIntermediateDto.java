package org.whu.backend.dto.ai;

import lombok.Data;

/**
 * 用于解析AI返回的中间结果的DTO
 */
@Data
public class AiRecoIntermediateDto {
    private String id;
    private String reason;
}