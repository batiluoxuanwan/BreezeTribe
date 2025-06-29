package org.whu.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.repository.post.TravelPostRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ViewCountService {
    private static final String VIEW_COUNT_KEY_PREFIX = "view:package:"; // Redis Key的前缀
    private static final long VIEW_COUNT_LOCK_MINUTES = 10; // 同一个IP，只记一次浏览的时间窗口（分钟）

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TravelPackageRepository packageRepository;
    @Autowired
    private TravelPostRepository travelPostRepository;

    /**
     * [核心] 尝试增加旅行团的浏览量（带IP防刷）
     * @param id 旅行团ID
     * @param ipAddress 用户的IP地址
     * @param entityType 要增加的实体类型，在case里面执行
     */
    // 使用 REQUIRES_NEW 传播级别，确保此方法总是在一个新的、可读写的事务中执行
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementViewCountIfAbsent(String id, String ipAddress,EntityType entityType) {
        // 1. 构造用于IP防刷的Redis Key
        String redisKey = VIEW_COUNT_KEY_PREFIX + ":" + entityType.toString() +":" + id + ":ip:" + ipAddress;

        // 2. 尝试在Redis中设置这个Key
        // opsForValue().setIfAbsent() 是一个原子操作
        // 如果Key不存在，它会设置成功并返回true；如果Key已存在，它会设置失败并返回false。
        Boolean isNewView = redisTemplate.opsForValue().setIfAbsent(
                redisKey,
                "1", // 值是什么不重要，我们只关心Key是否存在
                VIEW_COUNT_LOCK_MINUTES, // 设置过期时间
                TimeUnit.MINUTES
        );


        if(Boolean.TRUE.equals(isNewView)){
            log.info("新浏览记录: 旅行团ID '{}', IP地址 '{}'. 准备更新 '{}' 浏览量。", id, ipAddress, entityType);
            switch (entityType){
                case TRAVEL_PACKAGE -> packageRepository.incrementViewCount(id);
                case POST -> travelPostRepository.incrementViewCount(id);
            }
        }
    }

    public enum EntityType{
        TRAVEL_PACKAGE,
        POST
    }
}