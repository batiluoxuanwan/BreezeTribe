package org.whu.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.entity.Notification;
import org.whu.backend.entity.travelpac.TravelDeparture;
import org.whu.backend.entity.travelpac.TravelOrder;
import org.whu.backend.repository.travelRepo.TravelDepartureRepository;
import org.whu.backend.repository.travelRepo.TravelOrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ScheduledService {
    @Autowired
    private TravelOrderRepository travelOrderRepository;
    @Autowired
    private TravelDepartureRepository travelDepartureRepository;
    @Autowired
    private NotificationService notificationService;

    /**
     * 【核心重构】这是一个定时任务方法
     * cron = "0 0 1 * * ?" 的意思是：在每天凌晨1点整，自动执行这个方法。
     * (秒 分 时 日 月 周)
     * 批量更新订单状态
     */
    @Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Shanghai") // 使用东八区时间
    @Transactional
    public void updateOrderStatusJob() {
        log.info("【订单定时任务】 开始执行[订单状态]更新任务...");
        LocalDateTime now = LocalDateTime.now();

        // 1. 更新 PAID -> ONGOING
        // 逻辑：如果当前时间已经超过了订单对应团期的出发时间，则将“已支付”状态更新为“进行中”。
        int ongoingCount = travelOrderRepository.updatePaidToOngoing(now);
        if (ongoingCount > 0) {
            log.info("【定时任务】 成功将 {} 个已支付订单更新为“进行中”状态。", ongoingCount);
        }

        // 2. 更新 ONGOING -> COMPLETED
        // 逻辑：如果当前时间已经超过了“出发时间 + 行程天数”，则将“进行中”状态更新为“已完成”。
        int completedCount = travelOrderRepository.updateOngoingToCompleted(now);
        if (completedCount > 0) {
            log.info("【定时任务】 成功将 {} 个进行中订单更新为“已完成”状态。", completedCount);
        }

        log.info("【定时任务】 订单状态更新任务执行完毕。");
    }

    /**
     * 【定时任务2】每日发送临行提醒
     * cron = "0 5 1 * * ?" 的意思是：在每天凌晨1点05分执行，错开上一个任务。
     */
    @Scheduled(cron = "0 5 1 * * ?", zone = "Asia/Shanghai")
    @Transactional
    public void sendDepartureRemindersJob() {
        log.info("【定时任务】 开始执行[发送临行提醒]任务...");
        LocalDateTime now = LocalDateTime.now();

        // 我们要查找的是“明天”出发的团期和订单。
        LocalDateTime startOfDay = now.toLocalDate().plusDays(1).atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().plusDays(1).atTime(23, 59, 59);

        // 1. 通知商家：名下有团期明天出发
        List<TravelDeparture> departuresStartingTomorrow = travelDepartureRepository.findDeparturesBetween(startOfDay, endOfDay);
        for (TravelDeparture departure : departuresStartingTomorrow) {
            String packageTitle = departure.getTravelPackage().getTitle();

            String description = String.format("提醒：您发布的产品 [%s] 有一个团期将于明天（%s）出发，请做好准备。",
                    packageTitle, departure.getDepartureDate().toLocalDate());

            notificationService.createAndSendNotification(
                    departure.getTravelPackage().getDealer(),
                    Notification.NotificationType.DEPARTURE_REMINDER,
                    description,
                    null,
                    null,
                    departure.getTravelPackage().getId()
            );
        }
        if (!departuresStartingTomorrow.isEmpty()) {
            log.info("【定时任务】成功为 {} 个即将出发的团期向商家发送了提醒。", departuresStartingTomorrow.size());
        }

        // 2. 通知用户：预订的订单明天出发
        List<TravelOrder> ordersStartingTomorrow = travelOrderRepository.findPaidOrdersBetween(startOfDay, endOfDay);
        for (TravelOrder order : ordersStartingTomorrow) {
            String packageTitle = order.getTravelDeparture().getTravelPackage().getTitle();
            String description = String.format("临行提醒：您预订的旅行产品 [%s] 将于明天（%s）出发，祝您旅途愉快！",
                    packageTitle, order.getTravelDeparture().getDepartureDate().toLocalDate());

            notificationService.createAndSendNotification(
                    order.getUser(),
                    Notification.NotificationType.DEPARTURE_REMINDER,
                    description,
                    null,
                    null,
                    order.getId()
            );
        }
        if (!ordersStartingTomorrow.isEmpty()) {
            log.info("【定时任务】成功为 {} 笔即将出发的订单向用户发送了提醒。", ordersStartingTomorrow.size());
        }
        log.info("【定时任务】[发送临行提醒]任务执行完毕。");
    }

    /**
     * 在每天凌晨0点10分，自动执行这个方法。
     * (秒 分 时 日 月 周)
     * 这样可以确保在新的一天开始时，所有昨天的团期都会被正确地标记为已结束。
     */
    @Scheduled(cron = "00 10 1 * * ?", zone = "Asia/Shanghai") // 使用东八区时间
    @Transactional
    public void updateExpiredDeparturesJob() {
        log.info("【定时任务】开始执行[过期团期状态更新]任务...");
        LocalDateTime now = LocalDateTime.now();

        // 调用仓库方法，批量更新所有出发时间在当前时间之前的、且状态仍为OPEN或CLOSED的团期
        int updatedCount = travelDepartureRepository.updateStatusToFinishedForExpiredDepartures(now);

        if (updatedCount > 0) {
            log.info("【定时任务】成功将 {} 个已过期的团期状态更新为“FINISHED”。", updatedCount);
        } else {
            log.info("【定时任务】ℹ本次没有需要更新状态的过期团期。");
        }
        log.info("【定时任务】[过期团期状态更新]任务执行完毕。");
    }
}