package org.whu.backend.service;

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
import org.whu.backend.dto.route.RouteDetailDto;
import org.whu.backend.dto.spot.SpotDetailDto;
import org.whu.backend.dto.travelpack.PackageDetailDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.Route;
import org.whu.backend.entity.Spot;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.repository.TravelPackageRepository;
import org.whu.backend.util.AliyunOssUtil;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)

public class PublicService {
    @Autowired
    private TravelPackageRepository travelPackageRepository;

    public static final long EXPIRE_TIME = 60 * 60 * 4;

    /**
     * 获取已发布的旅行团列表（分页）
     */
    public PageResponseDto<PackageSummaryDto> getPublishedPackages(PageRequestDto pageRequestDto) {
        log.info("开始查询已发布的旅行团列表，分页参数: {}", pageRequestDto);

        // 1. 从DTO创建JPA需要的分页和排序对象
        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        // 2. 调用Repository中定义好的方法，查询的是PUBLISHED的旅游团
        Page<TravelPackage> packagePage = travelPackageRepository.findByStatus(TravelPackage.PackageStatus.PUBLISHED, pageable);
        log.info("查询到 {} 条记录，总共 {} 页。", packagePage.getNumberOfElements(), packagePage.getTotalPages());

        // 3. 将查询到的 Page<Entity> 转换为 List<DTO>
        List<PackageSummaryDto> summaryDtos = packagePage.getContent().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());

        // 4. 使用Builder模式构建分页响应对象
        return PageResponseDto.<PackageSummaryDto>builder()
                .content(summaryDtos)
                .pageNumber(packagePage.getNumber() + 1)
                .pageSize(packagePage.getSize())
                .totalElements(packagePage.getTotalElements())
                .totalPages(packagePage.getTotalPages())
                .first(packagePage.isFirst())
                .last(packagePage.isLast())
                .numberOfElements(packagePage.getNumberOfElements())
                .build();
    }

    /**
     * 获取单个旅行团的详情
     */
    public PackageDetailDto getPackageDetails(String id) {
        log.info("开始查询ID为 '{}' 的旅行团详情。", id);

        // 1. 调用Repository中我们定义好的方法
        TravelPackage travelPackage = travelPackageRepository.findByIdAndStatus(id, TravelPackage.PackageStatus.PUBLISHED)
                .orElseThrow(() -> {
                    log.warn("查询失败：找不到ID为 '{}' 的旅行团，或该旅行团尚未发布。", id);
                    return new BizException("找不到ID为 " + id + " 的旅行团，或该旅行团尚未发布。");
                });

        log.info("成功查询到旅行团 '{}' 的详情。", travelPackage.getTitle());
        // 2. 将Entity转换为详细的DTO
        return convertToDetailDto(travelPackage);
    }


    // --- 私有的转换方法 (Entity -> DTO) ---

    private PackageSummaryDto convertToSummaryDto(TravelPackage entity) {
        PackageSummaryDto dto = new PackageSummaryDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setCoverImageUrl(entity.getCoverImageUrl());
        dto.setPrice(entity.getPrice());
        dto.setDurationInDays(entity.getDurationInDays());
        return dto;
    }

    private PackageDetailDto convertToDetailDto(TravelPackage entity) {
        PackageDetailDto detailDto = new PackageDetailDto();
        detailDto.setId(entity.getId());
        detailDto.setTitle(entity.getTitle());
        if (entity.getCoverImageUrl() != null)
            detailDto.setCoverImageUrl(AliyunOssUtil.generatePresignedGetUrl(entity.getCoverImageUrl(), EXPIRE_TIME, "image/resize,l_1600/quality,q_50"));
        detailDto.setPrice(entity.getPrice());
        detailDto.setDurationInDays(entity.getDurationInDays());
        detailDto.setDetailedDescription(entity.getDetailedDescription());

        // 转换嵌套的路线列表
        List<RouteDetailDto> routeDtos = entity.getRoutes().stream()
                .map(packageRoute -> {
                    Route routeEntity = packageRoute.getRoute(); // 从关联实体中获取真正的Route对象
                    RouteDetailDto routeDto = new RouteDetailDto();
                    routeDto.setId(routeEntity.getId());
                    routeDto.setName(routeEntity.getName());
                    routeDto.setDescription(routeEntity.getDescription());

                    // 转换每个路线下的景点列表
                    List<SpotDetailDto> spotDtos = routeEntity.getSpots().stream()
                            .map(routeSpot -> {
                                Spot spotEntity = routeSpot.getSpot(); // 从关联实体中获取真正的Spot对象
                                SpotDetailDto spotDto = new SpotDetailDto();
                                spotDto.setId(spotEntity.getId());
                                spotDto.setMapProviderUid(spotEntity.getMapProviderUid());
                                spotDto.setName(spotEntity.getName());
                                spotDto.setCity(spotEntity.getCity());
                                spotDto.setAddress(spotEntity.getAddress());
                                spotDto.setLongitude(spotEntity.getLongitude());
                                spotDto.setLatitude(spotEntity.getLatitude());
                                return spotDto;
                            })
                            .collect(Collectors.toList());
                    routeDto.setSpots(spotDtos);

                    return routeDto;
                })
                .collect(Collectors.toList());
        detailDto.setRoutes(routeDtos);

        return detailDto;
    }
}
