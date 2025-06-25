package org.whu.backend.dto.baidumap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaiduSuggestionResponseDto {
    private int status;
    private String message;
    private List<SuggestionResult> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SuggestionResult {
        private String name;
        private Location location;
        private String uid;
        private String province;
        private String city;
        private String district;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double lng;
        private double lat;
    }
}