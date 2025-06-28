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
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.favourite.FavoritePageReqDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.favourite.FavoriteRequestDto;
import org.whu.backend.dto.favourite.FavouriteDetailDto;
import org.whu.backend.dto.order.OrderCreateRequestDto;
import org.whu.backend.dto.order.OrderDetailDto;
import org.whu.backend.entity.*;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.repository.FavoriteRepository;
import org.whu.backend.repository.travelRepo.OrderRepository;
import org.whu.backend.repository.travelRepo.RouteRepository;
import org.whu.backend.repository.travelRepo.SpotRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.util.AccountUtil;
import org.whu.backend.util.JpaUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
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
        Integer capacity = travelPackage.getCapacity();
        Integer participants = travelPackage.getParticipants() == null ? 0 : travelPackage.getParticipants();
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
        orderRepository.save(order);

        // 4. 更新报名人数
        travelPackage.setParticipants(participants + order.getTravelerCount());
        travelPackageRepository.save(travelPackage);

        OrderDetailDto dto= new OrderDetailDto();
        dto.setTravelerCount(order.getTravelerCount());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus());
        dto.setUsername(user.getUsername());
        dto.setTravelPackageTitle(travelPackage.getTitle());
        dto.setOrderId(order.getId());
        return dto;
    }

    public boolean confirmPayment(String orderId) {
        User user = securityUtil.getCurrentUser();

        // 获取订单
        Order order = JpaUtil.getOrThrow(orderRepository, orderId, "订单不存在");
        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            throw new BizException("订单状态错误");
        }

        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);
        return true;
    }
    public boolean cancelOrder(String orderId) {
        User user = securityUtil.getCurrentUser();

        // 获取订单
        Order order = JpaUtil.getOrThrow(orderRepository, orderId, "订单不存在");

        // 检查状态
        if (order.getStatus() == Order.OrderStatus.CANCELED || order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new BizException("订单状态错误");
        }

        // 检查是否需要退款
        if (order.getStatus() == Order.OrderStatus.PAID) {
            // TODO: 执行退款逻辑
        }

        order.setStatus(Order.OrderStatus.CANCELED);
        orderRepository.save(order);
        return true;
    }

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

        switch (favoriteRequestDto.getItemType()) {
            case PACKAGE:
                JpaUtil.getOrThrow(travelPackageRepository, favoriteRequestDto.getItemId(), "旅行团不存在");
                break;
            case SPOT:
                JpaUtil.getOrThrow(spotRepository, favoriteRequestDto.getItemId(), "景点不存在");
                break;
            case ROUTE:
                JpaUtil.getOrThrow(routeRepository, favoriteRequestDto.getItemId(), "路线不存在");
                break;
            case POST:
                // TODO: 检查游记是否存在
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

    public boolean removeFavorite(FavoriteRequestDto favoriteRequestDto) {
        User user = securityUtil.getCurrentUser();
        // 2. 查找并删除对应的Favorite记录
        // 检查是否存在
        Optional<Favorite> existing = favoriteRepository.findByUserAndItemIdAndItemType(user, favoriteRequestDto.getItemId(), favoriteRequestDto.getItemType());
        if (existing.isEmpty()) {
            throw new BizException("收藏不存在");
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
                .map(order -> {
                    OrderDetailDto dto = new OrderDetailDto();
                    dto.setOrderId(order.getId());
                    dto.setTravelerCount(order.getTravelerCount());
                    dto.setTotalPrice(order.getTotalPrice());
                    dto.setStatus(order.getStatus());
                    dto.setUsername(user.getUsername());
                    dto.setTravelPackageTitle(order.getTravelPackage().getTitle());
                    return dto;
                }).toList();

        // 返回分页响应结果
        return PageResponseDto.<OrderDetailDto>builder()
                .content(content)
                .pageNumber(pageRequestDto.getPage())
                .pageSize(pageRequestDto.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .numberOfElements(orderPage.getNumberOfElements())
                .build();
    }
}
