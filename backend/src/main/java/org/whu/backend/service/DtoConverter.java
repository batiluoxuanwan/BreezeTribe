package org.whu.backend.service;


import org.springframework.stereotype.Component;
import org.whu.backend.dto.accounts.AuthorDto;
import org.whu.backend.dto.mediafile.MediaFileDto;
import org.whu.backend.dto.order.OrderSummaryForDealerDto;
import org.whu.backend.dto.post.PostDetailDto;
import org.whu.backend.dto.post.PostSummaryDto;
import org.whu.backend.dto.postcomment.CommentDto;
import org.whu.backend.dto.postcomment.CommentWithRepliesDto;
import org.whu.backend.dto.route.RouteDetailDto;
import org.whu.backend.dto.route.RouteSummaryDto;
import org.whu.backend.dto.spot.SpotDetailDto;
import org.whu.backend.dto.travelpack.PackageDetailDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.*;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpost.Comment;
import org.whu.backend.entity.travelpost.TravelPost;
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

    /**
     * 将Comment实体转换为简单的DTO (不带嵌套回复)
     */
    public CommentDto convertCommentToDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(UserConvertToAuthorDto(comment.getAuthor()))
                .parentId(comment.getParent().getId())
                .replyToUsername(comment.getParent() != null ? comment.getParent().getAuthor().getUsername() : null)
                .replyToUserId(comment.getParent() != null ? comment.getParent().getAuthor().getId() : null)
                .createdTime(comment.getCreatedTime())
                .build();
    }

    /**
     * 将Comment实体转换为带少量预览回复的DTO
     */
    public CommentWithRepliesDto convertCommentToDtoWithReplies(Comment comment, List<CommentDto> repliesPreview, long totalReplies) {
        return CommentWithRepliesDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(UserConvertToAuthorDto(comment.getAuthor()))
                .replyToUsername(comment.getParent() != null ? comment.getParent().getAuthor().getUsername() : null)
                .createdTime(comment.getCreatedTime())
                .repliesPreview(repliesPreview)
                .totalReplies(totalReplies)
                .build();
    }

    // 把用户USER信息转换为dto
    public AuthorDto UserConvertToAuthorDto(User author) {
        return AuthorDto.builder()
                .id(author.getId())
                .username(author.getUsername())
                .avatarUrl(AliyunOssUtil.generatePresignedGetUrl(author.getAvatarUrl(), EXPIRE_TIME, IMAGE_PROCESS))
                .build();
    }

    // 将TravelPost实体转换为摘要DTO
    public PostSummaryDto convertPostToSummaryDto(TravelPost post) {
        // 获取封面图URL
        String coverUrl = null;
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            // 取第一张图作为封面
            String objectKey = post.getImages().get(0).getMediaFile().getObjectKey();
            coverUrl = AliyunOssUtil.generatePresignedGetUrl(objectKey, EXPIRE_TIME, IMAGE_PROCESS);
        }

        // 景点信息
        SpotDetailDto spotDto = null;
        if (post.getSpot() != null) {
            spotDto = ConvertSpotToDetailDto(post.getSpot());
        }

        return PostSummaryDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .coverImageUrl(coverUrl)
                .author(UserConvertToAuthorDto(post.getAuthor()))
                .spot(spotDto)
                .likeCount(post.getLikeCount())
                .favoriteCount(post.getFavoriteCount())
                .commentCount(post.getCommentCount())
                .createdTime(post.getCreatedTime())
                .build();
    }

    // 将TravelPost实体转换为详细的DTO
    public PostDetailDto convertPostToDetailDto(TravelPost post) {
        // 转换作者信息
        String url = post.getAuthor().getAvatarUrl();
        if (url != null) {
            url = AliyunOssUtil.generatePresignedGetUrl(url, EXPIRE_TIME, IMAGE_PROCESS);
        }
        AuthorDto authorDto = UserConvertToAuthorDto(post.getAuthor());

        // 转换景点信息 (如果存在)
        SpotDetailDto spotDto = null;
        if (post.getSpot() != null) {
            spotDto = ConvertSpotToDetailDto(post.getSpot());
        }

        // 转换图片URL列表
        List<String> imageUrls = post.getImages().stream()
                .map(postImage -> {
                    String objectKey = postImage.getMediaFile().getObjectKey();
                    return AliyunOssUtil.generatePresignedGetUrl(objectKey, EXPIRE_TIME, IMAGE_PROCESS);
                })
                .collect(Collectors.toList());

        // 构建最终的DTO
        return PostDetailDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .author(authorDto)
                .spot(spotDto)
                .imageUrls(imageUrls)
                .likeCount(post.getLikeCount())
                .favoriteCount(post.getFavoriteCount())
                .commentCount(post.getCommentCount())
                .createdTime(post.getCreatedTime())
                .build();
    }


    // 将景点Spot实体转换为景点SpotDetailDto
    public SpotDetailDto ConvertSpotToDetailDto(Spot spot) {
        return SpotDetailDto.builder()
                .id(spot.getId())
                .mapProviderUid(spot.getMapProviderUid())
                .name(spot.getName())
                .city(spot.getCity())
                .address(spot.getAddress())
                .longitude(spot.getLongitude())
                .latitude(spot.getLatitude())
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