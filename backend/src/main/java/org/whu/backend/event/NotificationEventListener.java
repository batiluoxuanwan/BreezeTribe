package org.whu.backend.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.whu.backend.entity.Notification;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpac.TravelOrder;
import org.whu.backend.service.NotificationService;

@Component
@Slf4j
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService;

    /**
     * 监听“订单创建成功”事件
     */
    @Async // 使用 @Async 让通知在后台线程发送，不阻塞下单主流程！
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        TravelOrder order = event.getOrder();
        User user = order.getUser();
        String packageTitle = order.getTravelDeparture().getTravelPackage().getTitle();

        log.info("监听到订单创建事件，准备为用户 '{}' 发送通知。", user.getUsername());

        String description = String.format("您关于旅行产品 [%s] 的订单已经创建成功，请及时支付", packageTitle);
        notificationService.createAndSendNotification(
                user,
                Notification.NotificationType.ORDER_CREATED,
                description,
                null,
                null,
                order.getTravelDeparture().getTravelPackage().getId()
        );
    }

    /**
     * 在这里可以继续添加监听其他事件的方法
     * 比如监听“收到点赞”事件
     */
    /*
    @Async
    @EventListener
    public void handlePostLiked(PostLikedEvent event) {
        // ... 获取被点赞的用户和点赞者 ...
        // ... 调用 notificationService 发送被赞通知 ...
    }
    */
}