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
import org.whu.backend.dto.order.OrderForReviewDto;
import org.whu.backend.dto.user.InteractionStatusRequestDto;
import org.whu.backend.dto.user.InteractionStatusResponseDto;
import org.whu.backend.dto.user.ItemIdentifierDto;
import org.whu.backend.dto.user.ItemStatusDto;
import org.whu.backend.entity.*;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.repository.FavoriteRepository;
import org.whu.backend.repository.LikeRepository;
import org.whu.backend.repository.post.TravelPostRepository;
import org.whu.backend.repository.travelRepo.*;
import org.whu.backend.service.DtoConverter;
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

    public OrderDetailDto createOrder(OrderCreateRequestDto orderCreateRequestDto) {
        // 1. 获取当前登录用户
        User user = securityUtil.getCurrentUser();

        // 2. 获取 TravelPackage 且验证状态及库存
        TravelPackage travelPackage = JpaUtil.getOrThrow(
                travelPackageRepository,
                orderCreateRequestDto.getPackageId(),
                "旅行团不存在"
        );
        if (travelPackage.getStatus() != TravelPackage.PackageStatus.PUBLISHED) {
            throw new BizException("该旅行团目前不可报名");
        }
        // TODO: 可能会有并发带来的超售操作
        Integer capacity = travelPackage.getCapacity();
        int participants = travelPackage.getParticipants() == null ? 0 : travelPackage.getParticipants();
        if (capacity != null && participants >= capacity) {
            throw new BizException("旅行团报名人数已满");
        }

        // 3. 创建新订单
        Order order = new Order();
        order.setUser(user);
        order.setTravelPackage(travelPackage);
        order.setTravelerCount(orderCreateRequestDto.getTravelerCount());
        order.setTotalPrice(travelPackage.getPrice()
                .multiply(BigDecimal.valueOf(orderCreateRequestDto.getTravelerCount())));
        order.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        order.setContactName(orderCreateRequestDto.getContactName());
        order.setContactPhone(orderCreateRequestDto.getContactPhone());
        Order saved = orderRepository.save(order);

        // 4. 原子更新报名人数
        travelPackageRepository.addParticipantCount(travelPackage.getId(), order.getTravelerCount());

        return dtoConverter.convertOrderToDetailDto(saved);
    }

    // 确认支付一个订单
    public boolean confirmPayment(String orderId) {
        // User user = securityUtil.getCurrentUser();

        // 获取订单
        Order order = JpaUtil.getOrThrow(orderRepository, orderId, "订单不存在");
        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            throw new BizException("订单状态错误");
        }

        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);
        return true;
    }

    // 取消一个订单
    public boolean cancelOrder(String orderId) {
        // User user = securityUtil.getCurrentUser();

        // 获取订单
        Order order = JpaUtil.getOrThrow(orderRepository, orderId, "订单不存在");

        // 检查状态
        if (order.getStatus() == Order.OrderStatus.CANCELED
                || order.getStatus() == Order.OrderStatus.COMPLETED
                || order.getStatus() == Order.OrderStatus.ONGOING) {
            throw new BizException("订单状态错误");
        }

        // 检查是否需要退款
//        if (order.getStatus() == Order.OrderStatus.PAID) {
//            // TODO: 执行退款逻辑
//        }

        order.setStatus(Order.OrderStatus.CANCELED);
        // 减少参团人数
        travelPackageRepository.subParticipantCount(orderId, order.getTravelerCount());
        orderRepository.save(order);
        return true;
    }

    // 获取当前用户的所有订单列表（分页）
    @Transactional(readOnly = true)
    public PageResponseDto<OrderDetailDto> getMyOrders(String currentUserId, PageRequestDto pageRequestDto) {
        log.info("用户ID '{}' 正在查询自己的全部订单列表...", currentUserId);

        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy()));

        Page<Order> orderPage = orderRepository.findByUserId(currentUserId, pageable);

        List<OrderDetailDto> dtos = orderPage.getContent().stream()
                .map(dtoConverter::convertOrderToDetailDto)
                .collect(Collectors.toList());

        return dtoConverter.convertPageToDto(orderPage, dtos);
    }

    // [新增] 根据评价状态，获取用户的订单列表
    @Transactional(readOnly = true)
    public PageResponseDto<OrderForReviewDto> getOrdersByReviewStatus(String currentUserId, String status, PageRequestDto pageRequestDto) {
        log.info("用户ID '{}' 正在查询 '{}' 状态的订单列表...", currentUserId, status);

        // 1. 先找出该用户已经评价过的所有旅行团ID
        Set<String> reviewedPackageIds = packageCommentRepository.findTravelPackageIdsReviewedByUser(currentUserId);

        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(Sort.Direction.DESC, "createdTime"));

        Page<Order> orderPage;

        // 2. 根据前端传来的状态，调用不同的查询方法
        if ("REVIEWED".equalsIgnoreCase(status)) {
            orderPage = orderRepository.findByUserIdAndStatusAndTravelPackageIdIn(
                    currentUserId, Order.OrderStatus.COMPLETED, reviewedPackageIds, pageable
            );
        } else if ("ALL".equalsIgnoreCase(status)) { // 查询全部(ALL)
            orderPage = orderRepository.findByUserIdAndStatus(
                    currentUserId, Order.OrderStatus.COMPLETED, pageable
            );
        } else { // 默认为查询“待评价” (PENDING_REVIEW)
            orderPage = orderRepository.findByUserIdAndStatusAndTravelPackageIdNotIn(
                    currentUserId, Order.OrderStatus.COMPLETED, reviewedPackageIds, pageable
            );
        }

        // 3. 将查询结果转换为DTO
        List<OrderForReviewDto> dtos = orderPage.getContent().stream()
                .map(dtoConverter::convertOrderToReviewDto)
                .collect(Collectors.toList());

        return dtoConverter.convertPageToDto(orderPage, dtos);
    }

    @Transactional // 必须加上事务注解！！！！！！
    public boolean addFavorite(FavoriteRequestDto favoriteRequestDto) {
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
                JpaUtil.getOrThrow(travelPostRepository, favoriteRequestDto.getItemId(), "游记不存在");
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
                .map(fav -> {
                    FavouriteDetailDto dto = new FavouriteDetailDto();
                    dto.setItemid(fav.getItemId());
                    dto.setItemType(fav.getItemType());
                    dto.setCreatedTime(fav.getCreatedTime());
                    dto.setUsername(user.getUsername());
                    dto.setUserid(user.getId());
                    return dto;
                }).toList();

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

        switch (likeRequestDto.getItemType()) {
            case POST:
                JpaUtil.getOrThrow(travelPostRepository, likeRequestDto.getItemId(), "游记不存在");
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
        return dtoConverter.convertPageToDto(orderPage,content);
    }
}
