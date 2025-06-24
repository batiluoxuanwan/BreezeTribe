package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.favourite.FavoriteRequestDto;
import org.whu.backend.dto.favourite.FavouriteDetailDto;
import org.whu.backend.dto.order.OrderCreateRequestDto;
import org.whu.backend.dto.order.OrderDetailDto;

@Tag(name = "用户-订单与收藏", description = "用户进行报名、收藏等操作的API")
@RestController
@RequestMapping("/api/user")
// @PreAuthorize("hasRole('USER')") // TODO: 在类级别上统一进行权限控制
public class UserActionController {

    @Operation(summary = "报名参加一个旅行团（创建订单）")
    @PostMapping("/orders")
    public Result<OrderDetailDto> createOrder(@Valid @RequestBody OrderCreateRequestDto orderCreateRequestDto) {
        // TODO: 业务逻辑
        // 1. 获取当前登录用户的ID
        // 2. 验证旅行团ID是否存在且可报名
        // 3. (可选)检查库存或人数限制
        // 4. 创建新的Order实体并保存
        // 5. 返回创建成功的订单详情
        return Result.success();
    }

    @Operation(summary = "取消参加一个旅行团（取消订单）")
    @DeleteMapping("/orders/{id}")
    public Result<OrderDetailDto> createOrder(@PathVariable String id) {
        // TODO: 业务逻辑
        return Result.success();
    }

    @Operation(summary = "收藏一个项目（旅行团、景点等）")
    @PostMapping("/favorites")
    public Result<?> addFavorite(@Valid @RequestBody FavoriteRequestDto favoriteRequestDto) {
        // TODO: 业务逻辑
        // 1. 获取当前登录用户的ID
        // 2. 根据favoriteRequestDto中的itemType和itemId，验证项目是否存在
        // 3. 创建并保存Favorite记录
        return Result.success("收藏成功");
    }

    @Operation(summary = "取消收藏一个项目")
    @DeleteMapping("/favorites")
    public Result<?> removeFavorite(@Valid @RequestBody FavoriteRequestDto favoriteRequestDto) {
        // TODO: 业务逻辑
        // 1. 获取当前登录用户的ID
        // 2. 查找并删除对应的Favorite记录
        return Result.success("取消收藏成功");
    }

    @Operation(summary = "获取我的收藏列表（分页）")
    @GetMapping("/favorites")
    public Result<PageResponseDto<FavouriteDetailDto>> getMyFavorites(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // TODO: 业务逻辑
        // 1. 获取当前登录用户的ID
        // 2. 分页查询该用户的收藏记录
        return Result.success();
    }
}