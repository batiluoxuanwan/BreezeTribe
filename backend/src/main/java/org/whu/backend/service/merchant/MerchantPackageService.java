package org.whu.backend.service.merchant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.baidumap.BaiduPlaceDetailResponseDto;
import org.whu.backend.dto.order.OrderSummaryForDealerDto;
import org.whu.backend.dto.travelpack.DayScheduleDto;
import org.whu.backend.dto.travelpack.PackageCreateRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.dto.travelpack.PackageUpdateRequestDto;
import org.whu.backend.entity.*;
import org.whu.backend.entity.accounts.Merchant;
import org.whu.backend.entity.association.PackageImage;
import org.whu.backend.entity.association.PackageRoute;
import org.whu.backend.entity.association.RouteSpot;
import org.whu.backend.repository.MediaFileRepository;
import org.whu.backend.repository.authRepo.MerchantRepository;
import org.whu.backend.repository.travelRepo.OrderRepository;
import org.whu.backend.repository.travelRepo.RouteRepository;
import org.whu.backend.repository.travelRepo.SpotRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.whu.backend.service.BaiduMapService;
import org.whu.backend.service.DtoConverter;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
public class MerchantPackageService {

    @Autowired
    private TravelPackageRepository packageRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private MediaFileRepository mediaFileRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private SpotRepository spotRepository;
    @Autowired
    private BaiduMapService baiduMapService;
    @Autowired
    private DtoConverter dtoConverter;

    @Transactional
    public TravelPackage createPackage(PackageCreateRequestDto dto, String dealerId) {
        log.info("经销商ID '{}' 正在创建新旅行团 '{}'...", dealerId, dto.getTitle());

        // 1. 查找经销商实体
        Merchant dealer = merchantRepository.findById(dealerId)
                .orElseThrow(() -> new BizException("找不到ID为 " + dealerId + " 的经销商"));

        // 2. 创建并填充旅行团基本信息
        TravelPackage newPackage = new TravelPackage();
        newPackage.setTitle(dto.getTitle());
        newPackage.setDetailedDescription(dto.getDetailedDescription());
        newPackage.setPrice(dto.getPrice());
        newPackage.setCapacity(dto.getCapacity());
        newPackage.setParticipants(0);
        newPackage.setDepartureDate(dto.getDepartureDate().truncatedTo(ChronoUnit.DAYS)); // 截断到天
        newPackage.setDurationInDays(dto.getDurationInDays());
        newPackage.setDealer(dealer);
        newPackage.setStatus(TravelPackage.PackageStatus.PENDING_APPROVAL); // 初始状态为待审核

        // 2.a. 处理并关联图片集
        if (dto.getImgIds() != null && !dto.getImgIds().isEmpty()) {
            for (int i = 0; i < dto.getImgIds().size(); i++) {
                String imgId = dto.getImgIds().get(i);
                MediaFile mediaFile = mediaFileRepository.findById(imgId)
                        .orElseThrow(() -> new BizException("找不到ID为 " + imgId + " 的媒体文件"));

                // 权限校验：确保经销商只能用自己上传的图片
                if (!mediaFile.getUploader().getId().equals(dealerId)) {
                    log.warn("权限拒绝：经销商ID '{}' 试图使用不属于自己的图片ID '{}'。", dealerId, imgId);
                    throw new BizException(HttpStatus.UNAUTHORIZED.value(),"不能使用不属于自己的图片");
                }

                // 创建关联实体
                PackageImage packageImage = new PackageImage();
                packageImage.setTravelPackage(newPackage);
                packageImage.setMediaFile(mediaFile);
                packageImage.setSortOrder(i + 1); // 按前端给的顺序排序
                newPackage.getImages().add(packageImage);

                // 将列表中的第一张图设置为封面图
                if (i == 0) {
                    // 我们直接使用静态工具类来获取URL
                    newPackage.setCoverImageUrl(mediaFile.getObjectKey());
                }
            }
        }

        // 2. [核心重构]处理每日行程，并隐式创建路线
        for (DayScheduleDto scheduleDto : dto.getDailySchedules()) {
            // 为每一天的行程，创建一个新的、私有的Route对象
            Route dailyRoute = new Route();
            dailyRoute.setName(scheduleDto.getRouteName());
            dailyRoute.setDescription(scheduleDto.getRouteDescription());
            dailyRoute.setDealer(dealer); // 路线的所有者也是这个经销商

            // 为这条新路线填充景点
            for (int i = 0; i < scheduleDto.getSpotUids().size(); i++) {
                String uid = scheduleDto.getSpotUids().get(i);
                Spot spot = findOrCreateSpotByBaiduUid(uid);

                RouteSpot routeSpot = new RouteSpot();
                routeSpot.setRoute(dailyRoute);
                routeSpot.setSpot(spot);
                routeSpot.setOrderColumn(i + 1);
                dailyRoute.getSpots().add(routeSpot);
            }

            // 先保存这个新创建的Route，使其获得一个持久化的ID
            Route savedRoute = routeRepository.save(dailyRoute);

            // 再创建旅行团和这条新路线的关联
            PackageRoute packageRoute = new PackageRoute();
            packageRoute.setTravelPackage(newPackage);
            packageRoute.setRoute(savedRoute);
            packageRoute.setDayNumber(scheduleDto.getDayNumber());
            newPackage.getRoutes().add(packageRoute);
        }

        // 4. 保存旅行团
        TravelPackage savedPackage = packageRepository.save(newPackage);
        log.info("新旅行团 '{}' (ID: {}) 已成功创建，状态为待审核。", savedPackage.getTitle(), savedPackage.getId());
        return savedPackage;
    }


    // 更新旅行团的核心业务逻辑，只允许修改描述和标题
    @Transactional
    public TravelPackage updatePackage(String packageId, PackageUpdateRequestDto dto, String currentDealerId) {
        log.info("经销商ID '{}' 正在尝试更新旅行团ID '{}'...", currentDealerId, packageId);

        // 1. 查找并验证旅行团的所有权
        TravelPackage packageToUpdate = findPackageByIdAndVerifyOwnership(packageId, currentDealerId);

        // 2. 更新允许修改的字段
        packageToUpdate.setTitle(dto.getTitle());
        packageToUpdate.setDetailedDescription(dto.getDetailedDescription());

        // 3. 重要！每次修改后，都需要重新进入待审核状态
        packageToUpdate.setStatus(TravelPackage.PackageStatus.PENDING_APPROVAL);

        // 4. 保存更新
        TravelPackage updatedPackage = packageRepository.save(packageToUpdate);
        log.info("旅行团ID '{}' 已被成功更新，状态重置为待审核。", packageId);
        return updatedPackage;
    }

    // [重构] 删除自己的一个旅行团（级联删除）
    @Transactional
    public void deletePackage(String packageId, String currentDealerId) {
        log.info("经销商ID '{}' 正在尝试删除旅行团ID '{}'...", currentDealerId, packageId);

        // 1. 查找并验证旅行团的所有权
        TravelPackage packageToDelete = findPackageByIdAndVerifyOwnership(packageId, currentDealerId);

        boolean hasActiveOrders = orderRepository.existsByTravelPackageId(packageId);
        if (hasActiveOrders) {
            log.warn("删除失败：旅行团ID '{}' 存在有效的关联订单。", packageId);
            throw new BizException("无法删除：该旅行团尚有未完成或未取消的订单，请先处理相关订单。");
        }

        // 2. [核心修改] 在删除旅行团之前，先手动删除它关联的所有路线
        log.info("正在级联删除旅行团 '{}' 关联的路线...", packageToDelete.getTitle());
        for (PackageRoute packageRoute : packageToDelete.getRoutes()) {
            Route routeToDelete = packageRoute.getRoute();
            if (routeToDelete != null) {
                // 因为Route与RouteSpot也是级联的，所以这里会一并删除RouteSpot的记录
                routeRepository.delete(routeToDelete);
                log.info(" -> 已删除路线ID: {}", routeToDelete.getId());
            }
        }

        // 3. 最后，删除旅行团本身。
        // @OneToMany的cascade和orphanRemoval会在这里生效，自动删除package_routes和package_images里的关联记录
        packageRepository.delete(packageToDelete);

        log.info("旅行团ID '{}' 及其所有关联数据已成功删除。", packageId);
    }

    // 获取自己创建的旅行团列表（分页）
    @Transactional(readOnly = true)
    public PageResponseDto<PackageSummaryDto> getMyPackages(String currentDealerId, PageRequestDto pageRequestDto) {
        log.info("经销商ID '{}' 正在获取自己的旅行团列表, 分页参数: {}", currentDealerId, pageRequestDto);

        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        Page<TravelPackage> packagePage = packageRepository.findByDealerId(currentDealerId, pageable);

        // 使用Lambda表达式来调用另一个Service中的方法，或者直接改成
        List<PackageSummaryDto> summaryDtos = packagePage.getContent().stream()
                .map(dtoConverter::convertPackageToSummaryDto)
                .collect(Collectors.toList());

        return dtoConverter.convertPageToDto(packagePage, summaryDtos);
    }



    // 获取指定旅行团的订单列表（分页）
    @Transactional(readOnly = true)
    public PageResponseDto<OrderSummaryForDealerDto> getPackageOrders(String packageId, String currentDealerId, PageRequestDto pageRequestDto) {
        log.info("经销商ID '{}' 正在获取旅行团ID '{}'的订单列表, 分页参数: {}", currentDealerId, packageId, pageRequestDto);

        // 1. 验证该旅行团是否属于当前经销商
        findPackageByIdAndVerifyOwnership(packageId, currentDealerId);

        // 2. 创建分页和排序对象
        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        // 3. 查询状态为“已支付”的订单
        Page<Order> orderPage = orderRepository.findByTravelPackageIdAndStatus(packageId, Order.OrderStatus.PAID, pageable);

        // 4. 将实体列表转换为对经销商安全的DTO列表
        List<OrderSummaryForDealerDto> dtos = orderPage.getContent().stream()
                .map(dtoConverter::convertOrderToSummaryForDealerDto)
                .collect(Collectors.toList());

        // 5. 封装并返回分页结果
        return dtoConverter.convertPageToDto(orderPage, dtos);
    }


    // 核心方法，实现了“按需缓存”景点的逻辑，公共便于复用
    public Spot findOrCreateSpotByBaiduUid(String uid) {
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

    // 一个私有辅助方法，用于查找旅行团并验证所有权
    private TravelPackage findPackageByIdAndVerifyOwnership(String packageId, String currentDealerId) {
        TravelPackage travelPackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new BizException("找不到ID为 " + packageId + " 的旅行团"));

        if (!travelPackage.getDealer().getId().equals(currentDealerId)) {
            log.warn("权限拒绝：经销商ID '{}' 试图操作不属于自己的旅行团ID '{}'。", currentDealerId, packageId);
            throw new BizException("无权操作此旅行团");
        }
        return travelPackage;
    }
}
