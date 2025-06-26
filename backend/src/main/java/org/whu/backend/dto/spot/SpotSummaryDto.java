package org.whu.backend.dto.spot;

import lombok.Data;

@Data
public class SpotSummaryDto {
    private String id; // 36位UUID
    private String mapProviderUid; // 外部地图供应商的唯一UID，百度地图

    private String name; // 景点名

    private String address; // 地址

    private String city; // 所在城市

    private Double longitude; //经度

    private Double latitude; // 纬度

    public SpotSummaryDto(String id, String name, String address, String city, Double longitude, Double latitude) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.longitude = longitude;
        this.latitude = latitude;
    }
}
