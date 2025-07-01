package org.whu.backend.dto.ai;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;


/**
 * 用于接收流式响应中每一个数据块(chunk)的DTO
 * 它告诉Jackson：“如果JSON里有多余的字段，而我这个类里没有，请直接忽略它们，不要报错。”
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIChunkResponseDto {
    private List<Choice> choices;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Delta delta;
        private String finish_reason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        private String content;
    }
}