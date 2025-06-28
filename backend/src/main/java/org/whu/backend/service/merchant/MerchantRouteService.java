package org.whu.backend.service.merchant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.route.RouteCreateRequestDto;
import org.whu.backend.dto.route.RouteDetailDto;
import org.whu.backend.dto.route.RouteSummaryDto;
import org.whu.backend.dto.route.RouteUpdateRequestDto;
import org.whu.backend.dto.baidumap.BaiduPlaceDetailResponseDto;
import org.whu.backend.entity.Route;
import org.whu.backend.entity.Spot;
import org.whu.backend.entity.accounts.Merchant;
import org.whu.backend.entity.association.RouteSpot;
import org.whu.backend.repository.travelRepo.RouteRepository;
import org.whu.backend.repository.travelRepo.SpotRepository;
import org.whu.backend.repository.authRepo.MerchantRepository;
import org.whu.backend.service.BaiduMapService;
import org.whu.backend.service.DtoConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    @Autowired
    private DtoConverter dtoConverter;

    @Transactional // 整个方法是一个事务，保证数据一致性
    public RouteDetailDto createRoute(RouteCreateRequestDto dto, String dealerId) {
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
        return dtoConverter.convertRouteToDetailDto(savedRoute);
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

    // --- 删除路线的业务逻辑 ---
    @Transactional
    public void deleteRoute(String routeId, String currentDealerId) {
        log.info("经销商ID '{}' 正在尝试删除路线ID '{}'...", currentDealerId, routeId);

        // 1. 查找路线是否存在
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> {
                    log.warn("删除失败：找不到ID为 '{}' 的路线。", routeId);
                    return new BizException("找不到ID为 " + routeId + " 的路线");
                });

        // 2. 关键！验证这条路线是否属于当前操作的经销商
        if (!route.getDealer().getId().equals(currentDealerId)) {
            log.error("权限拒绝：经销商ID '{}' 试图删除不属于自己的路线ID '{}'。", currentDealerId, routeId);
            throw new BizException("无权删除此路线");
        }

        // 3. 执行删除（因为有@SoftDelete，这会是逻辑删除）
        routeRepository.delete(route);
        log.info("路线ID '{}' 已被经销商ID '{}' 成功删除。", routeId, currentDealerId);
    }

    // 修改路线信息
    @Transactional
    public RouteDetailDto updateRoute(String routeId, RouteUpdateRequestDto dto, String currentDealerId) {
        log.info("经销商ID '{}' 正在尝试更新路线ID '{}'...", currentDealerId, routeId);
        // [修改] 调用新的私有方法，它包含了权限验证
        Route routeToUpdate = findRouteByIdAndVerifyOwnership(routeId, currentDealerId);

        // 更新基本信息
        routeToUpdate.setName(dto.getName());
        routeToUpdate.setDescription(dto.getDescription());

        // 更新景点列表
        updateRouteSpots(routeToUpdate, dto.getSpotUids());

        Route updatedRoute = routeRepository.save(routeToUpdate);
        log.info("路线ID '{}' 已被成功更新。", routeId);
        return dtoConverter.convertRouteToDetailDto(updatedRoute);
    }


    // 获取当前经销商创建的路线列表（分页）
    @Transactional(readOnly = true)
    public PageResponseDto<RouteSummaryDto> getMyRoutes(String currentDealerId, PageRequestDto pageRequestDto) {
        log.info("经销商ID '{}' 正在获取自己的路线列表, 分页参数: {}", currentDealerId, pageRequestDto);

        // 1. 创建分页和排序对象
        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        // 2. 调用Repository的新方法进行查询
        Page<Route> routePage = routeRepository.findByDealerId(currentDealerId, pageable);

        // 3. 将实体列表转换为DTO列表
        List<RouteSummaryDto> summaryDtos = routePage.getContent().stream()
                .map(dtoConverter::convertRouteToSummaryDto)
                .collect(Collectors.toList());

        // 4. 封装成自定义的分页响应对象返回
        return PageResponseDto.<RouteSummaryDto>builder()
                .content(summaryDtos)
                .pageNumber(routePage.getNumber() + 1)
                .pageSize(routePage.getSize())
                .totalElements(routePage.getTotalElements())
                .totalPages(routePage.getTotalPages())
                .first(routePage.isFirst())
                .last(routePage.isLast())
                .numberOfElements(routePage.getNumberOfElements())
                .build();
    }

    // 获取单条路线的详细信息（含权限校验）
    @Transactional(readOnly = true)
    public RouteDetailDto getMyRouteDetails(String routeId, String currentDealerId) {
        log.info("经销商ID '{}' 正在获取路线ID '{}' 的详情...", currentDealerId, routeId);

        // 1. 查找路线并验证所有权
        Route route = findRouteByIdAndVerifyOwnership(routeId, currentDealerId);

        // 2. [修改] 调用公共服务中的转换方法，将实体转换为DTO
        return dtoConverter.convertRouteToDetailDto(route);
    }


    // 私有辅助方法，用于查找路线并验证所有权
    private Route findRouteByIdAndVerifyOwnership(String routeId, String currentDealerId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new BizException("找不到ID为 " + routeId + " 的路线"));

        // 关键的权限校验
        if (!route.getDealer().getId().equals(currentDealerId)) {
            log.error("权限拒绝：经销商ID '{}' 试图操作不属于自己的路线ID '{}'。", currentDealerId, routeId);
            throw new BizException("无权操作此路线");
        }
        return route;
    }

    // 私有辅助方法，用于更新路线中的景点列表
    private void updateRouteSpots(Route route, List<String> spotUids) {
        // 清空旧的景点关联
        // orphanRemoval=true 会自动删除中间表里的记录
        route.getSpots().clear();

        // 添加新的景点关联
        for (int i = 0; i < spotUids.size(); i++) {
            String uid = spotUids.get(i);
            Spot spot = findOrCreateSpotByBaiduUid(uid);

            RouteSpot routeSpot = new RouteSpot();
            routeSpot.setRoute(route);
            routeSpot.setSpot(spot);
            routeSpot.setOrderColumn(i + 1); // 设置顺序
            route.getSpots().add(routeSpot);
        }
    }
}