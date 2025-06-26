package org.whu.backend.service;


import org.springframework.stereotype.Component;
import org.whu.backend.dto.mediafile.MediaFileDto;
import org.whu.backend.dto.order.OrderSummaryForDealerDto;
import org.whu.backend.dto.route.RouteDetailDto;
import org.whu.backend.dto.route.RouteSummaryDto;
import org.whu.backend.dto.spot.SpotDetailDto;
import org.whu.backend.dto.travelpack.PackageDetailDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.*;
import org.whu.backend.util.AliyunOssUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO转换器组件
 * 这是一个独立的、无状态的组件，专门负责将实体(Entity)转换为数据传输对象(DTO)。
 * 所有的Service都应该注入这个组件来执行转换，从而避免Service之间的循环依赖。
 */
@Component
public class DtoConverter {

    public static final long EXPIRE_TIME = 60 * 60 * 4 * 1000;
    public static final String IMAGE_PROCESS = "image/resize,l_400/quality,q_50";

    // 将文件元信息实体MediaFile转换为MediaFileDto
    public MediaFileDto convertMediaFileToDto(MediaFile entity) {
        // 在转换时动态生成带签名的URL
        String signedUrl = AliyunOssUtil.generatePresignedGetUrl(entity.getObjectKey(), EXPIRE_TIME, IMAGE_PROCESS);

        return MediaFileDto.builder()
                .id(entity.getId())
                .url(signedUrl)
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .createdTime(entity.getCreatedTime())
                .build();
    }


    // 把旅游团路线实体Route转化为简略的信息RouteSummaryDto
    public RouteSummaryDto convertRouteToSummaryDto(Route entity) {
        return RouteSummaryDto
                .builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .numOfSpots(entity.getSpots() != null ? entity.getSpots().size() : 0)
                .build();
    }

    // 把旅游团路线实体Route转化为详细的信息RouteSummaryDto
    public RouteDetailDto convertRouteToDetailDto(Route route) {
        // 先转换内部嵌套的Spot列表
        List<SpotDetailDto> spotDtos = route.getSpots().stream()
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

        // 使用Builder模式构建并返回RouteDetailDto
        return RouteDetailDto.builder()
                .id(route.getId())
                .name(route.getName())
                .description(route.getDescription())
                .spots(spotDtos)
                .build();
    }

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

    // 把旅游团实体TravelPackage转化为详细的信息PackageSummaryDto
    public PackageDetailDto convertPackageToDetailDto(TravelPackage entity) {
        // 1. 先转换内部嵌套的路线和景点列表
        List<RouteDetailDto> routeDtos = entity.getRoutes().stream()
                // 直接调用MerchantRouteService中已经写好的、公共的转换方法
                .map(packageRoute -> convertRouteToDetailDto(packageRoute.getRoute()))
                .collect(Collectors.toList());

        // 2. 转换并生成签名的图片URL列表
        List<String> signedImageUrls = entity.getImages().stream()
                .map(packageImage -> {
                    String objectKey = packageImage.getMediaFile().getObjectKey();
                    return AliyunOssUtil.generatePresignedGetUrl(objectKey, EXPIRE_TIME, IMAGE_PROCESS);
                })
                .collect(Collectors.toList());

        // 3. 构建最外层的PackageDetailDto
        return PackageDetailDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .coverImageUrls(signedImageUrls)
                .price(entity.getPrice())
                .durationInDays(entity.getDurationInDays())
                .detailedDescription(entity.getDetailedDescription())
                .status(entity.getStatus().name())
                .routes(routeDtos)
                .build();
    }

    // 将Order实体转换为对经销商安全的摘要DTO的私有辅助方法
    public OrderSummaryForDealerDto convertOrderToSummaryForDealerDto(Order order) {
        return OrderSummaryForDealerDto.builder()
                .orderId(order.getId())
                .travelerCount(order.getTravelerCount())
                .totalPrice(order.getTotalPrice())
                .contactName(maskName(order.getContactName())) // 脱敏处理
                .contactPhone(maskPhone(order.getContactPhone())) // 脱敏处理
                .orderTime(order.getCreatedTime())
                .build();
    }

    // 姓名脱敏辅助方法，例如 "张三" -> "张**"
    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "";
        return name.charAt(0) + "**";
    }

    // 电话脱敏辅助方法，例如 "13812345678" -> "138****5678"
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return "手机号格式错误";
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}