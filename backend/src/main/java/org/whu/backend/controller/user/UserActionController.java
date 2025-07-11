package org.whu.backend.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
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
import org.whu.backend.dto.order.OrderForReviewDto;
import org.whu.backend.dto.order.TravelOrderDetailDto;
import org.whu.backend.dto.report.ReportCreateDto;
import org.whu.backend.dto.user.InteractionStatusRequestDto;
import org.whu.backend.dto.user.InteractionStatusResponseDto;
import org.whu.backend.service.user.UserService;
import org.whu.backend.util.AccountUtil;

@Tag(name = "用户-各种交互操作，订单收藏点赞举报等", description = "登录用户进行报名、收藏等各种交互操作的API")
@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('USER')") // 在类级别上统一进行权限控制
@Slf4j
public class UserActionController {

    @Autowired
    UserService userService;

    @Operation(summary = "报名参加一个旅行团（创建订单）", description = "【已重构】现在是针对一个具体的出发团期进行报名。")
    @PostMapping("/orders")
    public Result<TravelOrderDetailDto> createOrder(@Valid @RequestBody OrderCreateRequestDto orderCreateRequestDto) {
        log.info("请求日志：用户正在创建新订单，请求参数: {}", orderCreateRequestDto);
        // 调用的是已重构的 createOrder 方法
        TravelOrderDetailDto dto = userService.createOrder(orderCreateRequestDto);
        return Result.success("报名成功，请及时支付", dto);
    }

    @Operation(summary = "【模拟】支付一笔订单", description = "【已重构】订单支付成功后，团期状态可能会更新。")
    @PostMapping("/orders/{orderId}/confirm-payment")
    public Result<?> confirmPayment(@PathVariable String orderId) {
        log.info("请求日志：用户正在确认支付订单ID '{}'", orderId);
        userService.confirmPayment(orderId);
        return Result.success("支付成功");
    }

    @Operation(summary = "用户取消一笔订单", description = "【已重构】取消后会释放团期库存。")
    @PostMapping("/orders/{orderId}/cancel")
    public Result<?> cancelOrder(@PathVariable String orderId) {
        log.info("请求日志：用户正在取消订单ID '{}'", orderId);
        userService.cancelOrder(orderId,false);
        return Result.success("订单已取消");
    }

    /**
     *  获取用户的订单列表（按评价状态筛选）
     */
    @Operation(summary = "获取我的订单列表（按评价状态筛选）(返回的都是已完成的订单，分为已完成未评价和已完成已评价)")
    @GetMapping("/orders/for-review")
    public Result<PageResponseDto<OrderForReviewDto>> getMyOrdersForReview(
            @Parameter(description = "筛选状态: PENDING (待评价), REVIEWED (已评价)，ALL (全部)") @RequestParam(defaultValue = "PENDING") String status,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问获取 '{}' 状态的订单列表接口", currentUserId, status);

        PageResponseDto<OrderForReviewDto> resultPage = userService.getOrdersByReviewStatus(currentUserId, status, pageRequestDto);

        return Result.success(resultPage);
    }

    /**
     *  获取用户的所有订单列表（分页）
     */
    @Operation(summary = "获取我的所有订单列表（分页）")
    @GetMapping("/orders")
    public Result<PageResponseDto<TravelOrderDetailDto>> getMyOrders(
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问获取所有订单列表接口", currentUserId);

        PageResponseDto<TravelOrderDetailDto> resultPage = userService.getMyOrders(currentUserId, pageRequestDto);

        return Result.success(resultPage);
    }

    @Operation(summary = "收藏一个项目（旅行团、景点等）")
    @PostMapping("/favorites")
    public Result<?> addFavorite(@Valid @RequestBody FavoriteRequestDto favoriteRequestDto) {
        if(!userService.addFavorite(favoriteRequestDto))
            throw new BizException("收藏失败");
        return Result.success("收藏成功");
    }

    @Operation(summary = "取消收藏一个项目（只能收藏旅行团）")
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

//    @Operation(summary = "获取我的订单列表（分页）")
//    @GetMapping("/orders")
//    public Result<PageResponseDto<OrderDetailDto>> getMyOrders(@Valid @ParameterObject FavoritePageReqDto pageRequestDto) {
//        PageResponseDto<OrderDetailDto> dto=userService.getMyOrders(pageRequestDto);
//        if(dto==null)
//            throw new BizException("获取失败");
//        return Result.success("获取订单列表",dto);
//    }

    @Operation(summary = "点赞一个项目（旅行团、景点等）")
    @PostMapping("/likes")
    public Result<?> addLike(@Valid @RequestBody LikeRequestDto dto) {
        if(!userService.addLike(dto))
            throw new BizException("点赞失败");
        return Result.success("点赞成功");
    }

    @Operation(summary = "取消点赞一个项目")
    @DeleteMapping("/likes")
    public Result<?> removeLike(@Valid @RequestBody LikeRequestDto dto) {
        if(!userService.removeLike(dto))
            throw new BizException("取消点赞失败");
        return Result.success("取消点赞成功");
    }

    @Operation(summary = "获取我的点赞列表（分页）")
    @GetMapping("/likes")
    public Result<PageResponseDto<LikeDetailDto>> getMyLikes(@Valid @ParameterObject LikePageRequestDto pageRequestDto) {
        PageResponseDto<LikeDetailDto> dto=userService.getMyLikes(pageRequestDto);
        if(dto==null)
            throw new BizException("获取失败");
        return Result.success("获取点赞列表",dto);
    }

    @Operation(summary = "批量获取项目的互动状态（是否已点赞/收藏）")
    @PostMapping("/interactions/status")
    public Result<InteractionStatusResponseDto> getInteractionStatus(@Valid @RequestBody InteractionStatusRequestDto request) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问批量获取互动状态接口", currentUserId);

        InteractionStatusResponseDto response = userService.getInteractionStatus(request, currentUserId);

        return Result.success(response);
    }

    @Operation(summary = "提交一个举报",description = "同时用户会得到一条类型为REPORT_CREATED的通知，通知关联的实体id为举报内容的id" +
            "但是因为不知道是什么类型的，所以通知的content会填入举报项目的类型。具体的枚举值请点击Request body下面，Example Value旁边的Schema按钮查看")
    @PostMapping("/report")
    public Result<?> createReport(@Valid @RequestBody ReportCreateDto createDto) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        userService.createReport(createDto, currentUserId);
        return Result.success("举报已提交，感谢您的反馈！");
    }
}