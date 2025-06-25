package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.FavoritePageReqDto;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.favourite.FavoriteRequestDto;
import org.whu.backend.dto.favourite.FavouriteDetailDto;
import org.whu.backend.dto.order.OrderCreateRequestDto;
import org.whu.backend.dto.order.OrderDetailDto;
import org.whu.backend.entity.Order;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.service.UserService;
import org.whu.backend.util.SecurityUtil;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "用户-订单与收藏", description = "用户进行报名、收藏等操作的API")
@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('USER')") // 在类级别上统一进行权限控制
public class UserActionController {

    @Autowired
    UserService userService;

    @Operation(summary = "报名参加一个旅行团（创建订单）")
    @PostMapping("/orders")
    public Result<OrderDetailDto> createOrder(@Valid @RequestBody OrderCreateRequestDto orderCreateRequestDto) {
        OrderDetailDto dto=userService.createOrder(orderCreateRequestDto);
        if(dto==null)
            throw new BizException("报名失败");
        return Result.success("报名成功，待支付",dto);
    }

    @Operation(summary = "【模拟】假装已经成功支付一笔订单")
    @PostMapping("/orders/{orderId}/confirm-payment")
    public Result<?> confirmPayment(@PathVariable String orderId) {
        if(!userService.confirmPayment(orderId))
            throw new RuntimeException("支付失败");
        return Result.success("支付成功");
    }

    @Operation(summary = "用户取消一笔订单")
    @PostMapping("/orders/{orderId}/cancel")
    public Result<?> cancelOrder(@PathVariable String orderId) {
        if(!userService.cancelOrder(orderId))
            throw new BizException("取消订单失败");
        return Result.success("订单已取消");
    }

    @Operation(summary = "收藏一个项目（旅行团、景点等）")
    @PostMapping("/favorites")
    public Result<?> addFavorite(@Valid @RequestBody FavoriteRequestDto favoriteRequestDto) {
        if(!userService.addFavorite(favoriteRequestDto))
            throw new BizException("收藏失败");
        return Result.success("收藏成功");
    }

    @Operation(summary = "取消收藏一个项目")
    @DeleteMapping("/favorites")
    public Result<?> removeFavorite(@Valid @RequestBody FavoriteRequestDto favoriteRequestDto) {
        if(!userService.removeFavorite(favoriteRequestDto))
            throw new BizException("取消收藏失败");
        return Result.success("取消收藏成功");
    }

    @Operation(summary = "获取我的收藏列表（分页）")
    @GetMapping("/favorites")
    public Result<PageResponseDto<FavouriteDetailDto>> getMyFavorites(@Valid @ParameterObject FavoritePageReqDto pageRequestDto) {
        PageResponseDto<FavouriteDetailDto> dto=userService.getMyFavorites(pageRequestDto);
        if(dto==null)
            throw new BizException("获取失败");
        return Result.success("获取收藏列表",dto);
    }
}