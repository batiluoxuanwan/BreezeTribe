package org.whu.backend.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.whu.backend.entity.Notification;
import org.whu.backend.entity.accounts.Merchant;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpac.TravelOrder;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.event.order.OrderCancelledEvent;
import org.whu.backend.event.order.OrderConfirmedEvent;
import org.whu.backend.event.order.OrderCreatedEvent;
import org.whu.backend.event.post.PostCommentedEvent;
import org.whu.backend.event.post.PostLikedEvent;
import org.whu.backend.event.travelpac.PackageCommentedEvent;
import org.whu.backend.event.travelpac.PackageFavouredEvent;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.whu.backend.service.NotificationService;
import org.whu.backend.util.JpaUtil;

import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private TravelPackageRepository travelPackageRepository;


    // -----------------------系统通知----------------------

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

    /**
     * 监听“订单支付成功”事件
     */
    @Async
    @EventListener
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        TravelOrder order = event.getOrder();
        User user = order.getUser();
        Merchant merchant = order.getTravelDeparture().getTravelPackage().getDealer();
        String packageTitle = order.getTravelDeparture().getTravelPackage().getTitle();
        // -------通知用户------
        String description = String.format("您关于旅行产品 [%s] 的订单已成功支付", packageTitle);
        notificationService.createAndSendNotification(
                user,
                Notification.NotificationType.ORDER_PAID,
                description,
                null,
                null,
                order.getTravelDeparture().getTravelPackage().getId()
        );
        // -------通知有新订单------
        // log.info("监听到订单支付事件，准备为商家 '{}' 发送新订单通知。", merchant.getUsername());
        String descriptionToMerchant = String.format(
                "已成功支付了您的旅行团 [%s] 的订单，团期 [%s]。联系人姓名: [%s]，联系电话: [%s])",
                packageTitle,
                order.getTravelDeparture().getDepartureDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                order.getContactName(),   // 使用订单里的联系人姓名
                order.getContactPhone()  // 使用订单里的联系人电话
        );
        notificationService.createAndSendNotification(
                merchant, // 接收者是商家
                Notification.NotificationType.USER_PAID, // 使用一个新的、专门的通知类型
                descriptionToMerchant,
                null,
                user, // 触发事件的用户
                order.getTravelDeparture().getId() // 关联团期id
        );
    }

    /**
     * 监听“订单取消”事件
     */
    @Async
    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        TravelOrder order = event.getOrder();
        User user = order.getUser();
        String packageTitle = order.getTravelDeparture().getTravelPackage().getTitle();
        String description = String.format("您关于旅行产品 [%s] 的订单已经取消成功", packageTitle);
        notificationService.createAndSendNotification(
                user,
                Notification.NotificationType.ORDER_CANCELED,
                description,
                null,
                null,
                order.getTravelDeparture().getTravelPackage().getId()
        );
    }


    // -----------------------收藏点赞通知----------------------

    /**
     * 监听“旅行团收藏”事件
     */
    @Async
    @EventListener
    public void handlePackageFavoured(PackageFavouredEvent event) {
        TravelPackage travelPackage = JpaUtil.getOrThrow(travelPackageRepository, event.getPackageId(), "旅行团不存在");
        String packageTitle = travelPackage.getTitle();
        String description = String.format("收藏了你的旅行团 [%s]", packageTitle);
        notificationService.createAndSendNotification(
                travelPackage.getDealer(),
                Notification.NotificationType.NEW_PACKAGE_FAVORITE,
                description,
                null,
                event.getUser(),
                travelPackage.getId()
        );
    }

    /**
     * 监听“游记点赞”事件
     */
    @Async
    @EventListener
    public void handlePostLiked(PostLikedEvent event) {
        String postTitle = event.getPost().getTitle();
        String description = String.format("点赞了你的游记 [%s]", postTitle);
        notificationService.createAndSendNotification(
                event.getPost().getAuthor(),
                Notification.NotificationType.NEW_POST_LIKE,
                description,
                null,
                event.getUser(),
                event.getPost().getId()
        );
    }

    // -----------------------评论回复通知----------------------

    /**
     * 监听“游记评论”事件
     */
    @Async
    @EventListener
    public void handlePostCommented(PostCommentedEvent event) {
        String postTitle = event.getPost().getTitle();
        String content = event.getContent();
        String description = String.format("在游记 [%s] 中回复了你：[%s]", postTitle, content);
        notificationService.createAndSendNotification(
                event.getCommentReceiver(),
                Notification.NotificationType.NEW_POST_COMMENT,
                description,
                content,
                event.getCommentSender(),
                event.getPost().getId()
        );
    }

    /**
     * 监听“旅行团评论”事件
     */
    @Async
    @EventListener
    public void handlePackageCommented(PackageCommentedEvent event){
        String packageTitle = event.getTravelPackage().getTitle();
        String content = event.getContent();
        String description = String.format("在旅行团评价 [%s] 中回复了你：[%s]", packageTitle, content);
        notificationService.createAndSendNotification(
                event.getCommentReceiver(),
                Notification.NotificationType.NEW_PACKAGE_COMMENT,
                description,
                content,
                event.getCommentSender(),
                event.getTravelPackage().getId()
        );
    }
}