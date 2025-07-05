package org.whu.backend.dto.travelpack;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用于查询产品是否可被编辑的状态DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CanUpdateStatusDto {
    @Schema(description = "是否可以更新")
    private boolean canUpdate;

    @Schema(description = "如果不可以更新，这里会提供原因")
    private String reason;
}