package org.whu.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.whu.backend.entity.Favorite;

import java.util.List;

@Data
@Builder
public class PageResponseDto<T> {

    @Schema(description = "当前页数据列表")
    private List<T> content; // 当前页的数据

    @Schema(description = "当前页码 (从1开始)")
    private int pageNumber;    // 当前页码 (基于1)

    @Schema(description = "每页记录数")
    private int pageSize;      // 每页大小

    @Schema(description = "总记录数")
    private long totalElements; // 总记录数

    @Schema(description = "总页数")
    private int totalPages;    // 总页数

    @Schema(description = "是否是第一页")
    private boolean first;       // 是否是第一页

    @Schema(description = "是否是最后一页")
    private boolean last;        // 是否是最后一页

    @Schema(description = "当前页实际记录数")
    private int numberOfElements; // 当前页的元素数量
}
