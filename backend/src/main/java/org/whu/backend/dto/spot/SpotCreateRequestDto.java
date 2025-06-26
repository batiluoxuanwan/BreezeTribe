package org.whu.backend.dto.spot;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class SpotCreateRequestDto {
    private String mapProviderUid; // 外部地图供应商的唯一UID，百度地图

    private String name; // 景点名

    private String address; // 地址

    private String city; // 所在城市

    private Double longitude; //经度

    private Double latitude; // 纬度
}
