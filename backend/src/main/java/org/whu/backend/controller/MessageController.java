package org.whu.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.whu.backend.entity.association.friend.Message;
import org.whu.backend.repository.authRepo.friend.MessageRepository;
import org.whu.backend.util.AccountUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @GetMapping("/{friendId}")
    public List<Message> getHistory(@PathVariable String friendId) {

        System.out.println("✅ 接口已进入");
        String accountId = AccountUtil.getCurrentAccountId();
        System.out.println("当前用户ID: " + accountId);
        System.out.println("好友ID: " + friendId);
        List<Message> fromMe = messageRepository.findByFromAccountIdAndToAccountId(accountId, friendId);
        List<Message> toMe = messageRepository.findByFromAccountIdAndToAccountId(friendId, accountId);

        List<Message> combined = new ArrayList<>();
        combined.addAll(fromMe);
        combined.addAll(toMe);

        // 按时间戳升序排序
        combined.sort(Comparator.comparing(Message::getTimestamp));
        System.out.println(combined);
        return combined;
        //return messageRepository.findByFromAccountIdAndToAccountIdOrViceVersa(accountId,friendId);
    }
}
