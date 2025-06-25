package org.whu.backend.dto.spot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略我们不需要的字段
public class BaiduPlaceDetailResponseDto {
    private int status;
    private String message;
    private List<Result> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String name;
        private Location location;
        private String address;
        private String city;
        private String area;
        private String uid;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double lng; // 经度
        private double lat; // 纬度
    }
}