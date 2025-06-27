//package org.whu.backend.controller;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//import org.whu.backend.common.Result;
//import org.whu.backend.dto.PageRequestDto;
//import org.whu.backend.dto.PageResponseDto;
//import org.whu.backend.dto.friend.FriendDto;
//import org.whu.backend.dto.friend.FriendRequestDto;
//import org.whu.backend.service.FriendService;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/friend")
//public class FriendController {
//
//    @Autowired
//    private FriendService friendService;
//
//    // 1️⃣ 发送好友请求
//    @PostMapping("/sendRequest")
//    public Result<?> sendRequest(@RequestParam String toAccountId) {
//        friendService.sendRequest(toAccountId);
//        return Result.success("好友请求已发送");
//    }
//
//    // 2️⃣ 获取当前账户的好友请求列表（分页）
//    @PostMapping("/receivedRequests")
//    public Result<PageResponseDto<FriendRequestDto>> getReceivedFriendRequests(@RequestBody PageRequestDto pageRequestDto) {
//        return Result.success(friendService.getReceivedFriendRequests(pageRequestDto));
//    }
//
//    // 3️⃣ 获取当前账户发送的好友请求列表（分页）
//    @PostMapping("/sentRequests")
//    public Result<PageResponseDto<FriendRequestDto>> getSentFriendRequests(@RequestBody PageRequestDto pageRequestDto) {
//        return Result.success(friendService.getSentFriendRequests(pageRequestDto));
//    }
//
//    // 4️⃣ 接受好友请求
//    @PostMapping("/acceptRequest")
//    public Result<?> acceptRequest(@RequestParam String requestId) {
//        friendService.acceptRequest(requestId);
//        return Result.success("好友请求已接受");
//    }
//
//    // 5️⃣ 拒绝好友请求
//    @PostMapping("/rejectRequest")
//    public Result<?> rejectRequest(@RequestParam String requestId) {
//        friendService.rejectRequest(requestId);
//        return Result.success("好友请求已拒绝");
//    }
//
//    // 6️⃣ 获取好友列表（分页）
//    @PostMapping("/friends")
//    public Result<PageResponseDto<FriendDto>> getFriends(@RequestBody PageRequestDto pageRequestDto) {
//        return Result.success(friendService.getFriends(pageRequestDto));
//    }
//
//    // 7️⃣ 删除好友
//    @DeleteMapping("/friend/{friendId}")
//    public Result<?> deleteFriend(@PathVariable String friendId) {
//        friendService.deleteFriend(friendId);
//        return Result.success("已删除好友");
//    }
//}
