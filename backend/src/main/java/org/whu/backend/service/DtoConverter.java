package org.whu.backend.service;


import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.accounts.AuthorDto;
import org.whu.backend.dto.accounts.UserProfileDto;
import org.whu.backend.dto.favourite.FavouriteDetailDto;
import org.whu.backend.dto.mediafile.MediaFileDto;
import org.whu.backend.dto.notification.NotificationDto;
import org.whu.backend.dto.order.OrderDetailDto;
import org.whu.backend.dto.order.OrderForReviewDto;
import org.whu.backend.dto.order.OrderSummaryForDealerDto;
import org.whu.backend.dto.order.TravelOrderDetailDto;
import org.whu.backend.dto.packagecomment.PackageCommentDto;
import org.whu.backend.dto.post.PostDetailDto;
import org.whu.backend.dto.post.PostDetailToOwnerDto;
import org.whu.backend.dto.post.PostSummaryDto;
import org.whu.backend.dto.postcomment.PostCommentDto;
import org.whu.backend.dto.postcomment.PostCommentWithRepliesDto;
import org.whu.backend.dto.route.RouteDetailDto;
import org.whu.backend.dto.route.RouteSummaryDto;
import org.whu.backend.dto.spot.SpotDetailDto;
import org.whu.backend.dto.travelpack.DepartureSummaryDto;
import org.whu.backend.dto.travelpack.PackageDetailDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.*;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpost.Comment;
import org.whu.backend.entity.Notification;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.util.AliyunOssUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
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


    public NotificationDto convertNotificationToDto(Notification notification) {
        String url = null;
        if (notification.getTriggerUser() != null && notification.getTriggerUser().getAvatarUrl() != null) {
            url = AliyunOssUtil.generatePresignedGetUrl(
                    notification.getTriggerUser().getAvatarUrl(), EXPIRE_TIME, IMAGE_PROCESS);
        }

        NotificationDto.NotificationDtoBuilder builder = NotificationDto.builder()
                .id(notification.getId())
                .isRead(notification.isRead())
                .type(notification.getType() != null ? notification.getType().toString() : null)
                .description(notification.getDescription() != null ? notification.getDescription() : null)
                .content(notification.getContent() != null ? notification.getContent() : null)
                .triggerUserAvatarUrl(url)
                .relatedItemId(notification.getRelatedItemId())
                .createdTime(notification.getCreatedTime());

        if (notification.getTriggerUser() != null) {
            builder.triggerUserId(notification.getTriggerUser().getId())
                    .triggerUsername(notification.getTriggerUser().getUsername());
        }

        return builder.build();
    }

    public FavouriteDetailDto convertFavoriteToDetailDto(Favorite favorite) {
        return FavouriteDetailDto.builder()
                .itemid(favorite.getItemId())
                .itemType(favorite.getItemType())
                .createdTime(favorite.getCreatedTime())
                .build();
    }

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
     * [新增] 一个通用的，将JPA的Page<T>对象转换为我们自定义PageResponseDto<U>的方法
     *
     * @param page    JPA返回的Page对象
     * @param content DTO列表，由调用方提前转换好
     * @param <T>     实体类型
     * @param <U>     DTO类型
     * @return 自定义的PageResponseDto
     */
    public <T, U> PageResponseDto<U> convertPageToDto(Page<T> page, List<U> content) {
        return PageResponseDto.<U>builder()
                .content(content)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }

    /**
     * [新增] 通用分页转换方法的重载版本，接收一个转换函数
     *
     * @param page      JPA返回的Page<T>对象
     * @param converter 一个能将实体T转换为DTO U的函数
     * @return 自定义的PageResponseDto<U>
     */
    public <T, U> PageResponseDto<U> convertPageToDto(Page<T> page, Function<T, U> converter) {
        // 1. 使用传入的转换函数，将实体列表转换为DTO列表
        List<U> dtoList = page.getContent().stream()
                .map(converter)
                .collect(Collectors.toList());

        // 2. 使用Builder模式构建并返回我们自定义的分页响应对象
        return PageResponseDto.<U>builder()
                .content(dtoList)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }

    /**
     * 将Order实体转换为详细的OrderDetailDto
     */
    public OrderDetailDto convertOrderToDetailDto(Order order) {
        return OrderDetailDto.builder()
                .orderId(order.getId())
                .packageId(order.getTravelPackage().getId())
                .packageTitle(order.getTravelPackage().getTitle())
                .packageCoverImageUrl(AliyunOssUtil.generatePresignedGetUrl(order.getTravelPackage().getCoverImageUrl(), EXPIRE_TIME, IMAGE_PROCESS))
                .travelerCount(order.getTravelerCount())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .orderTime(order.getCreatedTime())
                .build();
    }

    /**
     * 将Order实体转换为详细的OrderDetailDto
     */
    public TravelOrderDetailDto convertTravelOrderToDetailDto(TravelOrder order) {
        return TravelOrderDetailDto.builder()
                .id(order.getId())
                .departureId(order.getTravelDeparture().getId())
                .packageId(order.getTravelDeparture().getTravelPackage().getId())
                .packageTitle(order.getTravelDeparture().getTravelPackage().getTitle())
                .packageCoverImageUrl(AliyunOssUtil.generatePresignedGetUrl(
                        order.getTravelDeparture().getTravelPackage().getCoverImageUrl(), 
                        EXPIRE_TIME, 
                        IMAGE_PROCESS))
                .travelerCount(order.getTravelerCount())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().toString())
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .userId(order.getUser().getId())
                .orderTime(order.getCreatedTime())
                .build();
    }

    /**
     * 将订单实体转换为给用户查看的DTO
     */
    public OrderForReviewDto convertOrderToReviewDto(Order order) {
        return OrderForReviewDto.builder()
                .orderId(order.getId())
                .packageId(order.getTravelPackage().getId())
                .packageTitle(order.getTravelPackage().getTitle())
                .packageCoverImageUrl(AliyunOssUtil.generatePresignedGetUrl(order.getTravelPackage().getCoverImageUrl(), EXPIRE_TIME, IMAGE_PROCESS))
                .orderTime(order.getCreatedTime())
                .totalPrice(order.getTotalPrice().toString())
                .build();
    }



    /**
     * 将PackageComment实体转换为带少量预览回复的DTO
     */
    public PackageCommentDto convertPackageCommentToDto(PackageComment comment, List<PackageCommentDto> repliesPreview, long totalReplies) {
        return PackageCommentDto.builder()
                .id(comment.getId())
                .rating(comment.getRating())
                .content(comment.getContent())
                .author(ConvertUserToAuthorDto(comment.getAuthor()))
                .replyToUsername(comment.getParent() != null ? comment.getParent().getAuthor().getUsername() : null)
                .createdTime(comment.getCreatedTime())
                .repliesPreview(repliesPreview)
                .totalReplies(totalReplies)
                .build();
    }

    /**
     * [新增] 将PackageComment实体转换为不带嵌套回复的简单DTO
     */
    public PackageCommentDto convertPackageCommentToSimpleDto(PackageComment comment) {
        return PackageCommentDto.builder()
                .id(comment.getId())
                .rating(comment.getRating())
                .content(comment.getContent())
                .author(ConvertUserToAuthorDto(comment.getAuthor()))
                .replyToUsername(comment.getParent() != null ? comment.getParent().getAuthor().getUsername() : null)
                .createdTime(comment.getCreatedTime())
                .build();
    }

    /**
     * 将PostComment实体转换为简单的DTO (不带嵌套回复)
     */
    public PostCommentDto convertCommentToDto(Comment comment) {
        // 处理被屏蔽的评论
        String content = comment.getContent();
        if (comment.isDeletedByAuthor()) {
            content = "[该评论已被删除]";
        }
        return PostCommentDto.builder()
                .id(comment.getId())
                .content(content)
                .author(ConvertUserToAuthorDto(comment.getAuthor()))
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replyToUsername(comment.getParent() != null ? comment.getParent().getAuthor().getUsername() : null)
                .replyToUserId(comment.getParent() != null ? comment.getParent().getAuthor().getId() : null)
                .createdTime(comment.getCreatedTime())
                .build();
    }

    /**
     * 将PostComment实体转换为带少量预览回复的DTO
     */
    public PostCommentWithRepliesDto convertCommentToDtoWithReplies(Comment comment, List<PostCommentDto> repliesPreview, long totalReplies) {
        // 处理被屏蔽的评论
        String content = comment.getContent();
        if (comment.isDeletedByAuthor()) {
            content = "[该评论已被删除]";
        }
        return PostCommentWithRepliesDto.builder()
                .id(comment.getId())
                .content(content)
                .author(ConvertUserToAuthorDto(comment.getAuthor()))
                .replyToUsername(comment.getParent() != null ? comment.getParent().getAuthor().getUsername() : null)
                .createdTime(comment.getCreatedTime())
                .repliesPreview(repliesPreview)
                .totalReplies(totalReplies)
                .build();
    }

    // 把用户USER信息转换为dto
    public AuthorDto ConvertUserToAuthorDto(User author) {
        return AuthorDto.builder()
                .id(author.getId())
                .username(author.getUsername())
                .avatarUrl(AliyunOssUtil.generatePresignedGetUrl(author.getAvatarUrl(), EXPIRE_TIME, IMAGE_PROCESS))
                .build();
    }

    // 把用户信息转换为主页dto
    public UserProfileDto ConvertUserToUserProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(AliyunOssUtil.generatePresignedGetUrl(user.getAvatarUrl(), EXPIRE_TIME, IMAGE_PROCESS))
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
                .author(ConvertUserToAuthorDto(post.getAuthor()))
                .spot(spotDto)
                .likeCount(post.getLikeCount())
                .favoriteCount(post.getFavoriteCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .createdTime(post.getCreatedTime())
                .build();
    }

    // 将TravelPost实体转换为详细的DTO
    public PostDetailDto convertPostToDetailDto(TravelPost post) {
        // 转换作者信息
        AuthorDto authorDto = ConvertUserToAuthorDto(post.getAuthor());

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
                .viewCount(post.getViewCount())
                .createdTime(post.getCreatedTime())
                .build();
    }

    // 将TravelPost实体转换为面向拥有者的的DTO
    public PostDetailToOwnerDto convertPostToDetailToOwnerDto(TravelPost post) {
        // 转换作者信息
        AuthorDto authorDto = ConvertUserToAuthorDto(post.getAuthor());

        // 转换景点信息 (如果存在)
        SpotDetailDto spotDto = null;
        if (post.getSpot() != null) {
            spotDto = ConvertSpotToDetailDto(post.getSpot());
        }

        // 转换图片URL列表
        Map<String, String> imageIdAndUrls = post.getImages().stream()
                .collect(Collectors.toMap(
                        // 第一个参数：告诉它用什么做Map的Key
                        postImage -> postImage.getMediaFile().getId(),
                        // 第二个参数：告诉它用什么做Map的Value
                        postImage -> AliyunOssUtil.generatePresignedGetUrl(
                                postImage.getMediaFile().getObjectKey(),
                                EXPIRE_TIME,
                                IMAGE_PROCESS
                        )
                ));

        // 构建最终的DTO
        return PostDetailToOwnerDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .author(authorDto)
                .spot(spotDto)
                .imageIdAndUrls(imageIdAndUrls)
                .likeCount(post.getLikeCount())
                .favoriteCount(post.getFavoriteCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
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
        String coverImageUrl = null;
        if (entity.getCoverImageUrl() != null) {
            coverImageUrl = AliyunOssUtil.generatePresignedGetUrl(entity.getCoverImageUrl(), EXPIRE_TIME, IMAGE_PROCESS);
        }

        return PackageSummaryDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .coverImageUrl(coverImageUrl)
//                .price(entity.getPrice())
                .description(entity.getDetailedDescription())
                .durationInDays(entity.getDurationInDays())
                .favouriteCount(entity.getFavoriteCount())
                .commentCount(entity.getCommentCount())
                .viewCount(entity.getViewCount())
                .status(entity.getStatus().toString())
                .build();
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
                .favouriteCount(entity.getFavoriteCount())
                .commentCount(entity.getCommentCount())
                .viewCount(entity.getViewCount())
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

    public DepartureSummaryDto convertDepartureToSummaryDto(TravelDeparture departure) {
        DepartureSummaryDto.DepartureSummaryDtoBuilder builder = DepartureSummaryDto.builder()
                .id(departure.getId())
                .departureDate(departure.getDepartureDate())
                .price(departure.getPrice())
                .capacity(departure.getCapacity())
                .participants(departure.getParticipants())
                .status(departure.getStatus().name());
        if (departure.getTravelPackage() != null) {
            builder.packageId(departure.getTravelPackage().getId())
                   .packageTitle(departure.getTravelPackage().getTitle());
        }
        return builder.build();
    }
}