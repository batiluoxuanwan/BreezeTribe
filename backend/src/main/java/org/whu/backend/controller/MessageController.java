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

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @GetMapping("/{friendId}")
    public List<Message> getHistory(@PathVariable String friendId) {
        String accountId = AccountUtil.getCurrentAccountId();
        return messageRepository.findByFromAccountIdAndToAccountIdOrViceVersa(accountId,friendId);
    }
}
