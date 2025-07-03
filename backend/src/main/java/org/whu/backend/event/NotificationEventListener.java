package org.whu.backend.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.whu.backend.entity.Notification;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpac.TravelOrder;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.event.order.OrderCancelledEvent;
import org.whu.backend.event.order.OrderConfirmedEvent;
import org.whu.backend.event.order.OrderCreatedEvent;
import org.whu.backend.event.post.PostLikedEvent;
import org.whu.backend.event.travelpac.PackageFavouredEvent;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.whu.backend.service.NotificationService;
import org.whu.backend.service.auth.JwtService;
import org.whu.backend.util.JpaUtil;

@Component
@Slf4j
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private TravelPackageRepository travelPackageRepository;
    @Autowired
    private JwtService jwtService;

    /**
     * 监听“订单创建成功”事件
     */
    @Async // 使用 @Async 让通知在后台线程发送，不阻塞下单主流程！
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        TravelOrder order = event.getOrder();
        User user = order.getUser();
        String packageTitle = order.getTravelDeparture().getTravelPackage().getTitle();
//        log.info("监听到订单创建事件，准备为用户 '{}' 发送通知。", user.getUsername());
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


    @Async
    @EventListener
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        TravelOrder order = event.getOrder();
        User user = order.getUser();
        String packageTitle = order.getTravelDeparture().getTravelPackage().getTitle();
        String description = String.format("您关于旅行产品 [%s] 的订单已成功支付", packageTitle);
        notificationService.createAndSendNotification(
                user,
                Notification.NotificationType.ORDER_PAID,
                description,
                null,
                null,
                order.getTravelDeparture().getTravelPackage().getId()
        );
    }

    @Async
    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        TravelOrder order = event.getOrder();
        User user = order.getUser();
        String packageTitle = order.getTravelDeparture().getTravelPackage().getTitle();
        String description = String.format("您关于旅行产品 [%s] 的订单已经取消成功。", packageTitle);
        notificationService.createAndSendNotification(
                user,
                Notification.NotificationType.ORDER_CANCELED,
                description,
                null,
                null,
                order.getTravelDeparture().getTravelPackage().getId()
        );
    }

    @Async
    @EventListener
    public void handlePackageFavoured(PackageFavouredEvent event) {

        String userName = event.getUser().getUsername();
        TravelPackage travelPackage = JpaUtil.getOrThrow(travelPackageRepository, event.getPackageId(), "旅行团不存在");
        String packageTitle = travelPackage.getTitle();
        String description = String.format("用户 [%s] 收藏了你的旅行团 [%s] ", userName, packageTitle);
        notificationService.createAndSendNotification(
                travelPackage.getDealer(),
                Notification.NotificationType.NEW_PACKAGE_FAVORITE,
                description,
                null,
                event.getUser(),
                travelPackage.getId()
        );
    }

    @Async
    @EventListener
    public void handlePostLiked(PostLikedEvent event) {

        String userName = event.getUser().getUsername();
        String postTitle = event.getPost().getTitle();
        String description = String.format("用户 [%s] 点赞了你的游记 [%s] ", userName, postTitle);
        notificationService.createAndSendNotification(
                event.getPost().getAuthor(),
                Notification.NotificationType.NEW_POST_LIKE,
                description,
                null,
                event.getUser(),
                event.getPost().getId()
        );
    }
}