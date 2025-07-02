package org.whu.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.whu.backend.dto.friend.ChatMessageDto;
import org.whu.backend.entity.association.friend.Message;
import org.whu.backend.repository.authRepo.friend.MessageRepository;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageRepository messageRepository;

    @MessageMapping("/chat") // 监听/app/chat
    public void sendMessage(ChatMessageDto msg) {
        System.out.println("✅ 收到 WebSocket 消息: " + msg);

        // 保存消息
        Message message = new Message();
        message.setFromAccountId(msg.getFrom());
        message.setToAccountId(msg.getTo());
        message.setContent(msg.getContent());
        message.setTimestamp(LocalDateTime.now());
        message.setHadread(false);
        messageRepository.save(message);

        // 点对点推送消息到指定用户（假设订阅 /queue/messages）
        messagingTemplate.convertAndSendToUser(
                msg.getTo(), "/queue/messages", msg);
    }
}