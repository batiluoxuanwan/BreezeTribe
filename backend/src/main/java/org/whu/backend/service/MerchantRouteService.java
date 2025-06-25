package org.whu.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.route.RouteCreateRequestDto;
import org.whu.backend.dto.route.RouteSummaryDto;
import org.whu.backend.dto.spot.BaiduPlaceDetailResponseDto;
import org.whu.backend.entity.Route;
import org.whu.backend.entity.Spot;
import org.whu.backend.entity.accounts.Merchant;
import org.whu.backend.entity.association.RouteSpot;
import org.whu.backend.repository.travelRepo.RouteRepository;
import org.whu.backend.repository.travelRepo.SpotRepository;
import org.whu.backend.repository.authRepo.MerchantRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MerchantRouteService {

    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private SpotRepository spotRepository;
    @Autowired
    private MerchantRepository merchantRepository; // 用于查找经销商
    @Autowired
    private BaiduMapService baiduMapService;

    @Transactional // 整个方法是一个事务，保证数据一致性
    public RouteSummaryDto createRoute(RouteCreateRequestDto dto, String dealerId) {
        log.info("经销商ID '{}' 正在创建新路线 '{}'...", dealerId, dto.getName());

        // 1. 查找经销商实体
        Merchant dealer = merchantRepository.findById(dealerId)
                .orElseThrow(() -> new BizException("找不到ID为 " + dealerId + " 的经销商"));

        // 2. 处理景点列表
        List<Spot> spotsForRoute = new ArrayList<>();
        for (String uid : dto.getSpotUids()) {
            Spot spot = findOrCreateSpotByBaiduUid(uid);
            spotsForRoute.add(spot);
        }

        // 3. 创建并保存Route实体
        Route newRoute = new Route();
        newRoute.setName(dto.getName());
        newRoute.setDescription(dto.getDescription());
        newRoute.setDealer(dealer);

        // 4. 创建关联关系并设置顺序
        for (int i = 0; i < spotsForRoute.size(); i++) {
            Spot spot = spotsForRoute.get(i);
            RouteSpot routeSpot = new RouteSpot();
            routeSpot.setRoute(newRoute);
            routeSpot.setSpot(spot);
            routeSpot.setOrderColumn(i + 1); // 顺序从1开始
            newRoute.getSpots().add(routeSpot);
        }

        Route savedRoute = routeRepository.save(newRoute);
        log.info("新路线 '{}' (ID: {}) 已成功创建。", savedRoute.getName(), savedRoute.getId());
        return convertToSummaryDto(savedRoute);
    }

    /**
     * 这是一个核心的私有方法，实现了“按需缓存”景点的逻辑
     */
    private Spot findOrCreateSpotByBaiduUid(String uid) {
        // 先尝试从我们自己的数据库里找
        Optional<Spot> spotOptional = spotRepository.findByMapProviderUid(uid);

        if (spotOptional.isPresent()) {
            log.info("景点缓存命中，Baidu UID: {}", uid);
            return spotOptional.get();
        } else {
            // 如果没找到，就去请求百度API
            log.warn("景点缓存未命中，Baidu UID: {}。正在从外部API获取...", uid);
            BaiduPlaceDetailResponseDto.Result spotDetail = baiduMapService.getSpotDetailsByUid(uid)
                    .orElseThrow(() -> new BizException("无法获取百度地图UID为 " + uid + " 的有效景点信息"));

            // 将从百度获取的信息转换成我们自己的Spot实体并保存
            Spot newSpot = new Spot();
            newSpot.setMapProviderUid(spotDetail.getUid());
            newSpot.setName(spotDetail.getName());
            newSpot.setAddress(spotDetail.getAddress());
            newSpot.setCity(spotDetail.getCity());
            if (spotDetail.getLocation() != null) {
                newSpot.setLongitude(spotDetail.getLocation().getLng());
                newSpot.setLatitude(spotDetail.getLocation().getLat());
            }
            Spot savedSpot = spotRepository.save(newSpot);
            log.info("新景点 '{}' 已成功入库，ID: {}", savedSpot.getName(), savedSpot.getId());
            return savedSpot;
        }
    }

    // --- 私有的转换方法 (Entity -> DTO) ---
    // 把旅游团实体Route转化为简略的信息RouteSummaryDto
    private RouteSummaryDto convertToSummaryDto(Route entity) {
        return RouteSummaryDto
                .builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .numOfSpots(entity.getSpots().size())
                .build();
    }
}