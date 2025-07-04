package org.whu.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.repository.travelRepo.TravelOrderRepository;

import java.time.LocalDateTime;

@Service
@Slf4j
public class ScheduledService {
    @Autowired
    private TravelOrderRepository travelOrderRepository;

    /**
     * 【核心重构】这是一个定时任务方法
     * <p>
     * cron = "0 0 1 * * ?" 的意思是：在每天凌晨1点整，自动执行这个方法。
     * (秒 分 时 日 月 周)
     * 批量更新订单状态
     */
    @Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Shanghai") // 使用东八区时间
    @Transactional
    public void updateOrderStatusJob() {
        log.info("【定时任务 开始执行订单状态更新任务...");
        LocalDateTime now = LocalDateTime.now();

        // 1. 更新 PAID -> ONGOING
        // 逻辑：如果当前时间已经超过了订单对应团期的出发时间，则将“已支付”状态更新为“进行中”。
        int ongoingCount = travelOrderRepository.updatePaidToOngoing(now);
        if (ongoingCount > 0) {
            log.info("【定时任务 成功将 {} 个已支付订单更新为“进行中”状态。", ongoingCount);
        }

        // 2. 更新 ONGOING -> COMPLETED
        // 逻辑：如果当前时间已经超过了“出发时间 + 行程天数”，则将“进行中”状态更新为“已完成”。
        int completedCount = travelOrderRepository.updateOngoingToCompleted(now);
        if (completedCount > 0) {
            log.info("【定时任务 成功将 {} 个进行中订单更新为“已完成”状态。", completedCount);
        }

        log.info("【定时任务 订单状态更新任务执行完毕。");
    }
}