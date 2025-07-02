package org.whu.backend.service.user;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.favourite.FavoritePageReqDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.favourite.FavoriteRequestDto;
import org.whu.backend.dto.favourite.FavouriteDetailDto;
import org.whu.backend.dto.like.LikeDetailDto;
import org.whu.backend.dto.like.LikePageRequestDto;
import org.whu.backend.dto.like.LikeRequestDto;
import org.whu.backend.dto.order.OrderCreateRequestDto;
import org.whu.backend.dto.order.OrderDetailDto;
import org.whu.backend.dto.order.TravelOrderDetailDto;
import org.whu.backend.dto.user.InteractionStatusRequestDto;
import org.whu.backend.dto.user.InteractionStatusResponseDto;
import org.whu.backend.dto.user.ItemIdentifierDto;
import org.whu.backend.dto.user.ItemStatusDto;
import org.whu.backend.entity.*;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.Notification;
import org.whu.backend.entity.travelpac.TravelDeparture;
import org.whu.backend.entity.travelpac.TravelOrder;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.repository.FavoriteRepository;
import org.whu.backend.repository.LikeRepository;
import org.whu.backend.repository.post.TravelPostRepository;
import org.whu.backend.repository.travelRepo.*;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.NotificationService;
import org.whu.backend.util.AccountUtil;
import org.whu.backend.util.JpaUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class UserService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TravelOrderRepository travelOrderRepository;
    @Autowired
    private TravelDepartureRepository travelDepartureRepository;
    @Autowired
    private TravelPackageRepository travelPackageRepository;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private SpotRepository spotRepository;
    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private AccountUtil securityUtil;
    @Autowired
    private TravelPostRepository travelPostRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private DtoConverter dtoConverter;
    @Autowired
    private PackageCommentRepository packageCommentRepository;
    @Autowired
    private NotificationService notificationService;

    /**
     * 【核心重构】创建一个订单
     */
    @Transactional
    public TravelOrderDetailDto createOrder(OrderCreateRequestDto orderCreateRequestDto) {
        // 1. 获取当前登录用户
        User user = securityUtil.getCurrentUser();

        // 2. 【重大改变】根据 departureId 获取团期，而不是 packageId
        TravelDeparture departure = JpaUtil.getOrThrow(
                travelDepartureRepository,
                orderCreateRequestDto.getDepartureId(),
                "该出发团期不存在或已下架"
        );

        // 3. 验证团期状态和库存
        if (departure.getStatus() != TravelDeparture.DepartureStatus.OPEN) {
            throw new BizException("该团期当前不可报名（可能已报满或已结束）");
        }

        // 使用团期容量进行判断，并考虑并发安全
        int remainingCapacity = departure.getCapacity() - departure.getParticipants();
        if (orderCreateRequestDto.getTravelerCount() > remainingCapacity) {
            throw new BizException("团期剩余名额不足");
        }

        // 4. 创建新订单 (TravelOrder)
        TravelOrder order = new TravelOrder();
        order.setUser(user);
        order.setTravelDeparture(departure); // 关联到具体的团期
        order.setTravelerCount(orderCreateRequestDto.getTravelerCount());
        // 总价使用团期的价格计算
        order.setTotalPrice(departure.getPrice()
                .multiply(BigDecimal.valueOf(orderCreateRequestDto.getTravelerCount())));
        order.setStatus(TravelOrder.OrderStatus.PENDING_PAYMENT);
        order.setContactName(orderCreateRequestDto.getContactName());
        order.setContactPhone(orderCreateRequestDto.getContactPhone());
        TravelOrder savedOrder = travelOrderRepository.saveAndFlush(order);

        // 5. 【重大改变】原子更新团期的已报名人数
        int updatedRows = travelDepartureRepository.addParticipantCount(departure.getId(), order.getTravelerCount());
        if (updatedRows == 0) {
            // 如果更新失败（例如，并发情况下另一笔订单刚刚占用了名额），则回滚并抛出异常
            throw new BizException("报名失败，请重试（可能名额刚刚被抢完）");
        }
        log.info("服务层：团期ID '{}' 的报名人数增加 {}", departure.getId(), order.getTravelerCount());


        // 6. 发送通知 (通知内容可以从团期关联的产品中获取标题)
        String packageTitle = departure.getTravelPackage().getTitle();
        String description = String.format("您关于旅行产品 [%s] 的订单已经创建成功，请及时支付", packageTitle);
        notificationService.createAndSendNotification(
                user,
                Notification.NotificationType.ORDER_CREATED,
                description,
                null, // 根据需要设置关联ID
                null,
                departure.getTravelPackage().getId()
        );

        return dtoConverter.convertTravelOrderToDetailDto(savedOrder);
    }

    /**
     * 【已重构】确认支付一个订单
     */
    @Transactional
    public void confirmPayment(String orderId) {
        User user = securityUtil.getCurrentUser();
        TravelOrder order = JpaUtil.getOrThrow(travelOrderRepository, orderId, "订单不存在");

        // 权限校验：确保是用户自己的订单
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BizException("无权操作他人订单");
        }
        if (order.getStatus() != TravelOrder.OrderStatus.PENDING_PAYMENT) {
            throw new BizException("订单状态错误，无法支付");
        }

        order.setStatus(TravelOrder.OrderStatus.PAID);
        travelOrderRepository.save(order);
        log.info("服务层：订单ID '{}' 状态已更新为 PAID", orderId);

        // 发送支付成功通知
        String packageTitle = order.getTravelDeparture().getTravelPackage().getTitle();
        String description = String.format("您关于旅行产品 [%s] 的订单已成功支付", packageTitle);
        notificationService.createAndSendNotification(
                user,
                Notification.NotificationType.ORDER_PAID,
                description,
                null,
                null,
                order.getTravelDeparture().getTravelPackage().getId()
        );
    }

    /**
     * 【已重构】取消一个订单
     */
    @Transactional
    public void cancelOrder(String orderId) {
        User user = securityUtil.getCurrentUser();
        TravelOrder order = JpaUtil.getOrThrow(travelOrderRepository, orderId, "订单不存在");

        // 权限校验
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BizException("无权操作他人订单");
        }
        // 只有待支付和已支付的订单可以取消
        if (order.getStatus() != TravelOrder.OrderStatus.PENDING_PAYMENT && order.getStatus() != TravelOrder.OrderStatus.PAID) {
            throw new BizException("订单当前状态无法取消");
        }

        // 【重大改变】减少团期的已报名人数，释放库存
        travelDepartureRepository.subParticipantCount(order.getTravelDeparture().getId(), order.getTravelerCount());
        log.info("服务层：团期ID '{}' 的报名人数减少 {} (释放库存)", order.getTravelDeparture().getId(), order.getTravelerCount());

        order.setStatus(TravelOrder.OrderStatus.CANCELED);
        travelOrderRepository.save(order);
        log.info("服务层：订单ID '{}' 状态已更新为 CANCELED", orderId);

        // 发送取消成功通知
        String packageTitle = order.getTravelDeparture().getTravelPackage().getTitle();
        String description = String.format("您关于旅行产品 [%s] 的订单已经取消成功。", packageTitle);
        notificationService.createAndSendNotification(
                user,
                Notification.NotificationType.ORDER_CANCELED,
                description,
                null,
                null,
                order.getTravelDeparture().getTravelPackage().getId()
        );
    }

    // 获取当前用户的所有订单列表（分页）
    @Transactional(readOnly = true)
    public PageResponseDto<TravelOrderDetailDto> getMyOrders(String currentUserId, PageRequestDto pageRequestDto) {
        log.info("用户ID '{}' 正在查询自己的全部订单列表...", currentUserId);

        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy()));

        Page<TravelOrder> orderPage = travelOrderRepository.findByUserId(currentUserId, pageable);

        List<TravelOrderDetailDto> dtos = orderPage.getContent().stream()
                .map(dtoConverter::convertTravelOrderToDetailDto)
                .collect(Collectors.toList());

        return dtoConverter.convertPageToDto(orderPage, dtos);
    }

    // 根据评价状态，获取用户的订单列表 // TODO: 这个方法有BUG，还没修，查不出来已评价和未评价！！！！！
    @Transactional(readOnly = true)
    public PageResponseDto<TravelOrderDetailDto> getOrdersByReviewStatus(String currentUserId, String status, PageRequestDto pageRequestDto) {
        log.info("用户ID '{}' 正在查询 '{}' 状态的已完成订单列表...", currentUserId, status);

        // 1. 先找出该用户已经评价过的所有【产品模板】ID
        // 假设 packageCommentRepository 现在可以返回 TravelPackage 的 ID
        Set<String> reviewedPackageIds = packageCommentRepository.findTravelPackageIdsReviewedByUser(currentUserId);

        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(Sort.Direction.DESC, "createdTime"));

        Page<TravelOrder> orderPage;

        // 2. 【核心改变】根据前端传来的状态，调用新的、基于 TravelOrder 的查询方法
        // 注意：这里的查询逻辑现在是通过 TravelOrder -> TravelDeparture -> TravelPackage 进行关联
        if ("REVIEWED".equalsIgnoreCase(status)) {
            // 查询已完成的、且其关联的产品ID在“已评价列表”中的订单
            orderPage = travelOrderRepository.findByUserIdAndStatusAndTravelDeparture_TravelPackage_IdIn(
                    currentUserId, TravelOrder.OrderStatus.COMPLETED, reviewedPackageIds, pageable
            );
        } else if ("ALL".equalsIgnoreCase(status)) {
            // 查询全部已完成的订单
            orderPage = travelOrderRepository.findByUserIdAndStatus(
                    currentUserId, TravelOrder.OrderStatus.COMPLETED, pageable
            );
        } else { // 默认为查询“待评价” (PENDING)
            // 查询已完成的、且其关联的产品ID【不】在“已评价列表”中的订单
            orderPage = travelOrderRepository.findByUserIdAndStatusAndTravelDeparture_TravelPackage_IdNotIn(
                    currentUserId, TravelOrder.OrderStatus.COMPLETED, reviewedPackageIds, pageable
            );
        }

        // 3. 【核心改变】将查询结果转换为DTO，使用新的转换方法
        List<TravelOrderDetailDto> dtos = orderPage.getContent().stream()
                .map(dtoConverter::convertTravelOrderToDetailDto) // 使用新的转换方法
                .collect(Collectors.toList());

        return dtoConverter.convertPageToDto(orderPage, dtos);
    }

    @Transactional // 必须加上事务注解！！！！！！
    public boolean addFavorite(FavoriteRequestDto favoriteRequestDto) {

        // TODO: 仅允许对旅游团进行
        if (favoriteRequestDto.getItemType()!=InteractionItemType.PACKAGE){
            throw new BizException("仅允许对旅行团进行收藏");
        }

        User user = securityUtil.getCurrentUser();

        if (favoriteRequestDto.getItemType() == null || favoriteRequestDto.getItemId() == null) {
            throw new BizException("参数错误：itemType 或 itemId 为空");
        }
        Optional<Favorite> existingFavorite = favoriteRepository.findByUserAndItemIdAndItemType(
                user,
                favoriteRequestDto.getItemId(),
                favoriteRequestDto.getItemType()
        );
        if (existingFavorite.isPresent()) {
            throw new BizException("已经收藏过该对象");
        }

        TravelPost post = null;
        // 根据类型增加对应的统计数据
        switch (favoriteRequestDto.getItemType()) {
            case PACKAGE:
                JpaUtil.getOrThrow(travelPackageRepository, favoriteRequestDto.getItemId(), "旅行团不存在");
                travelPackageRepository.incrementFavoriteCount(favoriteRequestDto.getItemId());
                break;
            case SPOT:
                // TODO: 改成景点缓存逻辑
                JpaUtil.getOrThrow(spotRepository, favoriteRequestDto.getItemId(), "景点不存在");
                break;
            case ROUTE:
                JpaUtil.getOrThrow(routeRepository, favoriteRequestDto.getItemId(), "路线不存在");
                break;
            case POST:
                post = JpaUtil.getOrThrow(travelPostRepository, favoriteRequestDto.getItemId(), "游记不存在");
                travelPostRepository.incrementFavoriteCount(favoriteRequestDto.getItemId());
                break;
            default:
                throw new BizException("非法参数：未知的收藏类型");
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setItemId(favoriteRequestDto.getItemId());
        favorite.setItemType(favoriteRequestDto.getItemType());

        favoriteRepository.save(favorite);

        // 根据对应类型发送对应的通知
        switch (favoriteRequestDto.getItemType()) {
            case PACKAGE:
                break;
            case SPOT:
                break;
            case ROUTE:
                break;
            case POST:
                // 发送游记被收藏的通知
                log.info("给用户 {} 发送被游记被收藏的通知", user.getUsername());
                String description = String.format("%s 收藏了你的游记 %s", user.getUsername(), post.getTitle());
                notificationService.createAndSendNotification(
                        post.getAuthor(),
                        Notification.NotificationType.NEW_POST_FAVORITE,
                        description,
                        null,
                        user,
                        post.getId()
                );
                break;
            default:
                throw new BizException("非法参数：未知的收藏类型");
        }


        return true;
    }

    @Transactional // 必须加上事务注解！！！！！！
    public boolean removeFavorite(FavoriteRequestDto favoriteRequestDto) {
        User user = securityUtil.getCurrentUser();
        // 2. 查找并删除对应的Favorite记录
        // 检查是否存在
        Optional<Favorite> existing = favoriteRepository.findByUserAndItemIdAndItemType(user, favoriteRequestDto.getItemId(), favoriteRequestDto.getItemType());
        if (existing.isEmpty()) {
            throw new BizException("收藏不存在");
        }

        // 根据类型减少对应的统计数据
        switch (existing.get().getItemType()) {
            case PACKAGE:
                travelPackageRepository.decrementFavoriteCount(favoriteRequestDto.getItemId());
                break;
            case POST:
                travelPostRepository.decrementFavoriteCount(favoriteRequestDto.getItemId());
                break;
        }
        favoriteRepository.delete(existing.get());
        //favoriteRepository.deleteByUserAndItemIdAndItemType(user, favoriteRequestDto.getItemId(), favoriteRequestDto.getItemType());
        return true;
    }

    @Transactional
    public PageResponseDto<FavouriteDetailDto> getMyFavorites(@Valid @ParameterObject FavoritePageReqDto pageRequestDto) {
        User user = securityUtil.getCurrentUser();
        // 检查参数合法性
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());

        Pageable pageable = PageRequest.of(
                pageRequestDto.getPage() - 1,
                pageRequestDto.getSize(),
                Sort.by(direction, pageRequestDto.getSortBy())
        );

        Page<Favorite> favoritePage;
        if (pageRequestDto.getType() != null) {
            favoritePage = favoriteRepository.findByUserAndItemType(user, pageable, pageRequestDto.getType());
        } else {
            favoritePage = favoriteRepository.findByUser(user, pageable);
        }

        List<FavouriteDetailDto> content = favoritePage.getContent().stream()
                .map(dtoConverter::convertFavoriteToDetailDto).toList();

        return PageResponseDto.<FavouriteDetailDto>builder()
                .content(content)
                .pageNumber(pageRequestDto.getPage())
                .pageSize(pageRequestDto.getSize())
                .totalElements(favoritePage.getTotalElements())
                .totalPages(favoritePage.getTotalPages())
                .first(favoritePage.isFirst())
                .last(favoritePage.isLast())
                .numberOfElements(favoritePage.getNumberOfElements())
                .build();
    }


    // 用户进行点赞操作，目前只能点赞游记
    @Transactional // 必须加上事务注解！！！！！！
    public boolean addLike(LikeRequestDto likeRequestDto) {
        User user = securityUtil.getCurrentUser();

        if (likeRequestDto.getItemType() == null || likeRequestDto.getItemId() == null) {
            throw new BizException("参数错误：itemType 或 itemId 为空");
        }
        if (likeRequestDto.getItemType() != InteractionItemType.POST) {
            throw new BizException("仅允许对POST执行点赞操作");
        }
        Optional<Like> existingLike = likeRepository.findByUserAndItemIdAndItemType(
                user,
                likeRequestDto.getItemId(),
                likeRequestDto.getItemType()
        );
        if (existingLike.isPresent()) {
            throw new BizException("不能重复点赞");
        }

        TravelPost post = null;

        switch (likeRequestDto.getItemType()) {
            case POST:
                post = JpaUtil.getOrThrow(travelPostRepository, likeRequestDto.getItemId(), "游记不存在");
                travelPostRepository.incrementLikeCount(likeRequestDto.getItemId());
                break;
            default:
                throw new BizException("非法参数：未知的点赞类型");
        }

        Like like = new Like();
        like.setUser(user);
        like.setItemId(likeRequestDto.getItemId());
        like.setItemType(likeRequestDto.getItemType());

        likeRepository.save(like);

        // 发送游记被点赞的通知
        String description = String.format("%s 赞了你的游记 %s", user.getUsername(), post.getTitle());

        notificationService.createAndSendNotification(
                post.getAuthor(),
                Notification.NotificationType.NEW_POST_LIKE,
                description,
                null,
                user,
                post.getId()
        );

        return true;
    }

    // 用户取消点赞操作，目前只能点赞游记
    @Transactional // 必须加上事务注解！！！！！！
    public boolean removeLike(LikeRequestDto likeRequestDto) {
        User user = securityUtil.getCurrentUser();
        // 2. 查找并删除对应的点赞记录
        // 检查是否存在
        Optional<Like> existing = likeRepository.findByUserAndItemIdAndItemType(user, likeRequestDto.getItemId(), likeRequestDto.getItemType());
        if (existing.isEmpty()) {
            throw new BizException("点赞不存在");
        }
        switch (existing.get().getItemType()) {
            case POST:
                travelPostRepository.decrementLikeCount(likeRequestDto.getItemId());
                break;
        }
        likeRepository.delete(existing.get());
        return true;
    }

    // 用户查询自己的所有点赞
    @Transactional
    public PageResponseDto<LikeDetailDto> getMyLikes(@Valid @ParameterObject LikePageRequestDto pageRequestDto) {
        User user = securityUtil.getCurrentUser();
        // 检查参数合法性
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());

        Pageable pageable = PageRequest.of(
                pageRequestDto.getPage() - 1,
                pageRequestDto.getSize(),
                Sort.by(direction, pageRequestDto.getSortBy())
        );

        Page<Like> likePage;
        if (pageRequestDto.getType() != null) {
            likePage = likeRepository.findByUserAndItemType(user, pageable, pageRequestDto.getType());
        } else {
            likePage = likeRepository.findByUser(user, pageable);
        }

        List<LikeDetailDto> content = likePage.getContent().stream()
                .map(like -> {
                    LikeDetailDto dto = new LikeDetailDto();
                    dto.setItemid(like.getItemId());
                    dto.setItemType(like.getItemType());
                    dto.setCreatedTime(like.getCreatedTime());
                    dto.setUsername(user.getUsername());
                    dto.setUserid(user.getId());
                    return dto;
                }).toList();

        return dtoConverter.convertPageToDto(likePage, content);
    }


    // 用户查询自己的项目的互动状态
    @Transactional(readOnly = true)
    public InteractionStatusResponseDto getInteractionStatus(InteractionStatusRequestDto request, String currentUserId) {
        log.info("用户ID '{}' 正在批量查询 {} 个项目的互动状态", currentUserId, request.getItems().size());

        Map<String, ItemStatusDto> statusMap = new HashMap<>();

        // 1. 按类型对ID进行分组
        Map<InteractionItemType, Set<String>> itemIdsByType = request.getItems().stream()
                .collect(Collectors.groupingBy(
                        ItemIdentifierDto::getType,
                        Collectors.mapping(ItemIdentifierDto::getId, Collectors.toSet())
                ));

        // 2. 初始化所有请求项目的状态
        for (ItemIdentifierDto item : request.getItems()) {
            String key = item.getType().name() + "_" + item.getId();
            statusMap.put(key, ItemStatusDto.builder().isLiked(false).isFavorited(false).build());
        }

        // 3. 遍历所有需要查询的类型，分别处理收藏和点赞
        for (Map.Entry<InteractionItemType, Set<String>> entry : itemIdsByType.entrySet()) {
            InteractionItemType itemType = entry.getKey();
            Set<String> itemIds = entry.getValue();

            // 批量更新收藏状态
            Set<String> favoritedIds = favoriteRepository.findByUserIdAndItemTypeAndItemIdIn(currentUserId, itemType, itemIds)
                    .stream().map(Favorite::getItemId).collect(Collectors.toSet());
            for (String itemId : favoritedIds) {
                statusMap.get(itemType.name() + "_" + itemId).setFavorited(true);
            }

            // 业务规则：只有POST类型支持点赞
            if (itemType == InteractionItemType.POST) {
                Set<String> likedIds = likeRepository.findByUserIdAndItemTypeAndItemIdIn(currentUserId, itemType, itemIds)
                        .stream().map(Like::getItemId).collect(Collectors.toSet());
                for (String itemId : likedIds) {
                    statusMap.get(itemType.name() + "_" + itemId).setLiked(true);
                }
            }
        }

        return InteractionStatusResponseDto.builder().statusMap(statusMap).build();
    }

    // 准备废弃
    public PageResponseDto<OrderDetailDto> getMyOrders(@Valid FavoritePageReqDto pageRequestDto) {
        User user = securityUtil.getCurrentUser();

        // 构造分页与排序对象
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());
        Pageable pageable = PageRequest.of(
                pageRequestDto.getPage() - 1,
                pageRequestDto.getSize(),
                Sort.by(direction, pageRequestDto.getSortBy())
        );

        // 查询订单数据
        Page<Order> orderPage = orderRepository.findByUser(user, pageable);

        // 构造返回 DTO 列表
        List<OrderDetailDto> content = orderPage.getContent().stream()
                .map(dtoConverter::convertOrderToDetailDto).toList();

        // 返回分页响应结果
        return dtoConverter.convertPageToDto(orderPage, content);
    }
}
