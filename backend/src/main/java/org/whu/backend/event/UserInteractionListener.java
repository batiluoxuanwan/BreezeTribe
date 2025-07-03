package org.whu.backend.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.whu.backend.service.user.UserProfileService;

/**
 * 事件监听器
 * 它会“监听”应用中发生的各种用户行为事件，并异步调用画像服务进行更新。
 * 这样做的好处是，下单、点赞等主业务流程完全不受影响，性能更高。
 */
@Component
@Slf4j
public class UserInteractionListener {

    @Autowired
    private UserProfileService userProfileService;

    // @Async // 不使用 @Async 注解，防止懒加载出事
    @EventListener
    public void handleUserInteraction(UserInteractionEvent event) {
        log.info("监听到用户行为事件：用户ID '{}', 权重 '{}'", event.getUserId(), event.getWeight());
        userProfileService.updateUserProfile(event.getUserId(), event.getTags(), event.getWeight());
    }
}