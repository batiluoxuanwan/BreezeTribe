package org.whu.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.whu.backend.dto.spot.BaiduPlaceDetailResponseDto;

import java.util.Optional;

@Service
@Slf4j
public class BaiduMapService {

    // 从 application.properties 文件中注入你的百度地图AK
    @Value("${baidu.map.ak}")
    private String baiduMapAk;

    private final RestTemplate restTemplate;

    // 构造函数注入RestTemplate，这是推荐的做法
    public BaiduMapService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 根据百度地图的UID获取景点详情
     * @param uid 景点的百度地图UID
     * @return 包含景点详情的DTO，如果查询失败或无结果则返回Optional.empty()
     */
    public Optional<BaiduPlaceDetailResponseDto.Result> getSpotDetailsByUid(String uid) {
        String url = String.format(
                "https://api.map.baidu.com/place/v3/detail?uid=%s&scope=1&ak=%s",
                uid, baiduMapAk
        );

        try {
            log.info("向百度地图API发送请求");
            BaiduPlaceDetailResponseDto response = restTemplate.getForObject(url, BaiduPlaceDetailResponseDto.class);

            if (response != null && response.getStatus() == 0 && response.getResults() != null) {
                log.info("成功从百度地图获取到UID '{}' 的详情: {}", uid, response.getResults().get(0).getName());
                return Optional.of(response.getResults().get(0));
            } else {
                log.warn("从百度地图获取UID '{}' 的详情失败，响应: {}", uid, response);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("调用百度地图API时发生异常, UID: {}", uid, e);
            return Optional.empty();
        }
    }
}