package org.whu.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 *  批量查询互动状态的请求体
 */
@Data
public class InteractionStatusRequestDto {
    @Schema(description = "需要查询状态的项目列表")
    @NotEmpty
    private List<ItemIdentifierDto> items;
}