package org.whu.backend.dto.travelpack;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PackageUpdateRequestDto {
    @NotEmpty
    private String title;
    private String detailedDescription;
    private Integer durationInDays;

    @NotEmpty
    private List<DayScheduleDto> dailySchedules; // 新的行程安排

    @NotEmpty
    private List<String> imgIds; // 新的图片ID列表

    private List<String> tagIds; // 新的标签ID列表

}