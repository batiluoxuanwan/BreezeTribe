//package org.whu.backend.controller;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.*;
//import org.whu.backend.common.Result;
//
//import java.util.ArrayList;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/friend")
//public class FriendController {
//
//    // 1️⃣ 发送好友请求
//    @PostMapping("/sendRequest")
//    public Result<?> sendRequest(@RequestParam String toAccountId) {
//        // TODO: Service.sendRequest(...)
//        return Result.success("好友请求已发送");
//    }
//
//    // 2️⃣ 获取当前账户的好友请求列表
//    @GetMapping("/requests")
//    public Result<List<FriendRequestDTO>> getFriendRequests() {
//        // TODO: Service.getFriendRequests(...)
//        return Result.success(new ArrayList<>());
//    }
//
//    // 3️⃣ 接受好友请求
//    @PostMapping("/acceptRequest")
//    public Result<?> acceptRequest(@RequestParam String requestId) {
//        // TODO: Service.acceptRequest(...)
//        return Result.success("好友请求已接受");
//    }
//
//    // 4️⃣ 拒绝好友请求
//    @PostMapping("/rejectRequest")
//    public Result<?> rejectRequest(@RequestParam String requestId) {
//        // TODO: Service.rejectRequest(...)
//        return Result.success("好友请求已拒绝");
//    }
//
//    // 5️⃣ 获取好友列表
//    @GetMapping("/friends")
//    public Result<List<FriendDTO>> getFriends() {
//        // TODO: Service.getFriends(...)
//        return Result.success(new ArrayList<>());
//    }
//
//    // 6️⃣ 删除好友
//    @DeleteMapping("/friend/{friendId}")
//    public Result<?> deleteFriend(@PathVariable String friendId) {
//        // TODO: Service.deleteFriend(...)
//        return Result.success("已删除好友");
//    }
//}
