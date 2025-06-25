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
    public static final String IMAGE_PROCESS = "image/resize,l_1600/quality,q_50";

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
                .map(this::convertPackageToSummaryDto)
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

        // 1. 调用Repository中定义好的方法
        TravelPackage travelPackage = travelPackageRepository.findByIdAndStatus(id, TravelPackage.PackageStatus.PUBLISHED)
                .orElseThrow(() -> {
                    log.warn("查询失败：找不到ID为 '{}' 的旅行团，或该旅行团尚未发布。", id);
                    return new BizException("找不到ID为 " + id + " 的旅行团，或该旅行团尚未发布。");
                });
        log.info("成功查询到旅行团 '{}' 的详情。", travelPackage.getTitle());
        // 2. 将Entity转换为详细的DTO
        return convertPackageToDetailDto(travelPackage);
    }


    // --- 私有的转换方法 (Entity -> DTO) ---
    // 把旅游团实体TravelPackage转化为简略的信息PackageSummaryDto
    public PackageSummaryDto convertPackageToSummaryDto(TravelPackage entity) {
        PackageSummaryDto dto = new PackageSummaryDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        if (entity.getCoverImageUrl() != null)
            dto.setCoverImageUrl(AliyunOssUtil.generatePresignedGetUrl(entity.getCoverImageUrl(), EXPIRE_TIME, IMAGE_PROCESS));
        dto.setPrice(entity.getPrice());
        dto.setDescription(entity.getDetailedDescription());
        dto.setDurationInDays(entity.getDurationInDays());
        dto.setStatus(entity.getStatus().toString());
        return dto;
    }

    public PackageDetailDto convertPackageToDetailDto(TravelPackage entity) {
        // 1. 先转换内部嵌套的路线和景点列表
        List<RouteDetailDto> routeDtos = entity.getRoutes().stream()
                .map(packageRoute -> {
                    Route routeEntity = packageRoute.getRoute();

                    // 转换每个路线下的景点列表
                    List<SpotDetailDto> spotDtos = routeEntity.getSpots().stream()
                            .map(routeSpot -> {
                                Spot spotEntity = routeSpot.getSpot();
                                return SpotDetailDto.builder()
                                        .id(spotEntity.getId())
                                        .mapProviderUid(spotEntity.getMapProviderUid())
                                        .name(spotEntity.getName())
                                        .city(spotEntity.getCity())
                                        .address(spotEntity.getAddress())
                                        .longitude(spotEntity.getLongitude())
                                        .latitude(spotEntity.getLatitude())
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return RouteDetailDto.builder()
                            .id(routeEntity.getId())
                            .name(routeEntity.getName())
                            .description(routeEntity.getDescription())
                            .spots(spotDtos)
                            .build();
                })
                .collect(Collectors.toList());

        // 2. 构建最外层的PackageDetailDto
        return PackageDetailDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .coverImageUrl(
                        entity.getCoverImageUrl() == null ? null :
                        AliyunOssUtil.generatePresignedGetUrl(entity.getCoverImageUrl(), EXPIRE_TIME, IMAGE_PROCESS)
                )
                .price(entity.getPrice())
                .durationInDays(entity.getDurationInDays())
                .detailedDescription(entity.getDetailedDescription())
                .status(entity.getStatus().toString())
                .routes(routeDtos)
                .build();
    }
}
