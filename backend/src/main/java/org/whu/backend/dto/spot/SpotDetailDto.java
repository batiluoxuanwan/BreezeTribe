package org.whu.backend.dto.spot;
import lombok.Builder;
import lombok.Data;

/**
 * 景点信息 DTO (用于嵌套在RouteDto中)
 */
@Data
@Builder
public class SpotDetailDto {
    private String id;
    private String mapProviderUid;
    private String name;
    private String city;
    private String address;
    private Double longitude; //经度
    private Double latitude; // 纬度
}
