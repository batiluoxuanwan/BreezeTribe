package org.whu.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.whu.backend.dto.baidumap.BaiduPlaceDetailResponseDto;
import org.whu.backend.dto.baidumap.BaiduSuggestionResponseDto;

import java.util.Collections;
import java.util.List;
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



    /**
     * [新增] 获取地点输入提示
     * @param keyword 用户输入的关键词
     * @param region 搜索区域，如 "北京"
     * @return 地点建议列表
     */
    public List<BaiduSuggestionResponseDto.SuggestionResult> getSuggestions(String keyword, String region) {
        String url = String.format(
                "https://api.map.baidu.com/place/v3/suggestion?query=%s&region=%s&region_limit=true&output=json&ak=%s",
                keyword, region, baiduMapAk
        );

        try {
            log.info("BaiduMapService: 正在请求地点输入提示, 关键词: '{}', 区域: '{}'", keyword, region);
            BaiduSuggestionResponseDto response = restTemplate.getForObject(url, BaiduSuggestionResponseDto.class);

            if (response != null && response.getStatus() == 0 && response.getResults() != null) {
                log.info("BaiduMapService: 成功获取到 {} 条地点建议", response.getResults().size());
                return response.getResults();
            } else {
                log.warn("BaiduMapService: 获取地点建议失败, 关键词: '{}', 响应: {}", keyword, response);
                return Collections.emptyList(); // 返回空列表表示失败
            }
        } catch (Exception e) {
            log.error("BaiduMapService: 调用地点输入提示API时发生异常, 关键词: '{}'", keyword, e);
            return Collections.emptyList(); // 发生异常也返回空列表
        }
    }
}