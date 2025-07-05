package org.whu.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.accounts.ShareDto;
import org.whu.backend.dto.friend.FriendDto;
import org.whu.backend.dto.friend.FriendRequestDto;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.repository.authRepo.AuthRepository;
import org.whu.backend.repository.authRepo.friend.FriendShipRepository;
import org.whu.backend.service.FriendService;
import org.whu.backend.service.pub.PublicService;
import org.whu.backend.util.AccountUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/friend")
public class FriendController {

    @Autowired
    private FriendService friendService;
    @Autowired
    private PublicService publicService;
    @Autowired
    private FriendShipRepository friendShipRepository;
    @Autowired
    private AccountUtil accountUtil;
    @Autowired
    private AuthRepository authRepository;

    /**
     * 1️⃣ 发送好友请求
     */
    @Operation(summary = "发送好友请求", description = "向指定用户发送好友请求")
    @PostMapping("/sendRequest")
    public Result<?> sendRequest(
            @Parameter(description = "目标用户 ID") @RequestParam String toAccountId) {
        friendService.sendRequest(toAccountId);
        return Result.success("好友请求已发送");
    }

    /**
     * 2️⃣ 获取接收到的好友请求（分页）
     */
    @Operation(summary = "获取接收到的好友请求", description = "分页获取当前用户接收到的好友请求")
    @PostMapping("/receivedRequests")
    public Result<PageResponseDto<FriendRequestDto>> getReceivedFriendRequests(
            @Parameter(description = "分页请求参数") @RequestBody PageRequestDto pageRequestDto) {
        return Result.success(friendService.getReceivedFriendRequests(pageRequestDto));
    }

    /**
     * 3️⃣ 获取发送的好友请求（分页）
     */
    @Operation(summary = "获取已发送的好友请求", description = "分页获取当前用户发送的好友请求")
    @PostMapping("/sentRequests")
    public Result<PageResponseDto<FriendRequestDto>> getSentFriendRequests(
            @Parameter(description = "分页请求参数") @RequestBody PageRequestDto pageRequestDto) {
        return Result.success(friendService.getSentFriendRequests(pageRequestDto));
    }

    /**
     * 4️⃣ 接受好友请求
     */
    @Operation(summary = "接受好友请求", description = "通过请求 ID 接受一个好友请求")
    @PostMapping("/acceptRequest")
    public Result<?> acceptRequest(
            @Parameter(description = "好友请求 ID") @RequestParam String requestId) {
        friendService.acceptRequest(requestId);
        return Result.success("好友请求已接受");
    }

    /**
     * 5️⃣ 拒绝好友请求
     */
    @Operation(summary = "拒绝好友请求", description = "通过请求 ID 拒绝一个好友请求")
    @PostMapping("/rejectRequest")
    public Result<?> rejectRequest(
            @Parameter(description = "好友请求 ID") @RequestParam String requestId) {
        friendService.rejectRequest(requestId);
        return Result.success("好友请求已拒绝");
    }

    /**
     * 6️⃣ 获取当前用户的好友列表（分页）
     */
    @Operation(summary = "获取好友列表", description = "分页获取当前用户的好友列表")
    @PostMapping("/friends")
    public Result<PageResponseDto<FriendDto>> getFriends(
            @Parameter(description = "分页请求参数") @RequestBody PageRequestDto pageRequestDto) {
        return Result.success(friendService.getFriends(pageRequestDto));
    }

    /**
     * 7️⃣ 删除指定好友
     */
    @Operation(summary = "删除好友", description = "通过好友 ID 删除一位好友")
    @DeleteMapping("/friend/{friendId}")
    public Result<?> deleteFriend(
            @Parameter(description = "好友 ID") @PathVariable String friendId) {
        friendService.deleteFriend(friendId);
        return Result.success("已删除好友");
    }

    /**
     * 8️⃣ 搜索可以添加为好友的用户
     */
    @Operation(summary = "搜索可添加的用户", description = "根据用户 ID、手机号、邮箱进行精确查询，或根据用户名模糊查询，并排除已是好友的用户")
    @GetMapping("/search")
    public Result<List<ShareDto>> searchUsersCouldBeFriend(
            @Parameter(description = "关键词：可以是用户ID、手机号、邮箱或用户名") @RequestParam String keyword) {

        Account currentUser = accountUtil.getCurrentAccount();
        List<ShareDto> finalDto = new ArrayList<>();
        List<ShareDto> dto = publicService.searchUsers(keyword);

        for (ShareDto user : dto) {
            // 跳过自己
            if (user.getId().equals(currentUser.getId())) continue;

            Account otherUser = authRepository.findById(user.getId())
                    .orElseThrow(() -> new BizException("用户不存在"));

            // 如果不是好友才加入返回列表（注意双向检查）
            boolean isFriend = friendShipRepository.existsByAccount1AndAccount2(currentUser, otherUser)
                    || friendShipRepository.existsByAccount1AndAccount2(otherUser, currentUser);

            if (!isFriend) {
                finalDto.add(user);
            }
        }

        return Result.success(finalDto);
    }

    @Operation(summary = "是否为好友", description = "判断是不是用户的好友")
    @GetMapping("/areufriend")
    public Result<Boolean> areufriend(@RequestParam String hisid) {
        Account currentUser = accountUtil.getCurrentAccount();
        Account otherUser = authRepository.findById(hisid)
                .orElseThrow(() -> new BizException("用户不存在"));
        boolean isFriend = friendShipRepository.existsByAccount1AndAccount2(currentUser, otherUser);

        return Result.success(isFriend);
    }
}

