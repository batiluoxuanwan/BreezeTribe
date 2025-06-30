package org.whu.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.notification.MarkAsReadRequestDto;
import org.whu.backend.dto.notification.NotificationDto;
import org.whu.backend.dto.notification.UnreadCountDto;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.travelpost.Notification;
import org.whu.backend.repository.NotificationRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private DtoConverter dtoConverter;

    /**
     * [核心] 创建并发送一条新通知的统一方法
     *
     * @param recipient     接收者
     * @param type          通知的类型
     * @param content       通知的具体内容
     * @param triggerUser   通知的触发者，例如点赞的人
     * @param relatedItemId 相关的实体id，例如被点赞的游记
     */
    @Transactional
    public void createAndSendNotification(Account recipient, Notification.NotificationType type, String description, String content, Account triggerUser, String relatedItemId) {
        if (recipient == null) {
            log.warn("通知创建失败：接收者为空。");
            return;
        }

        // 自己触发的事件，不通知自己
        if (triggerUser != null && recipient.getId().equals(triggerUser.getId())) {
            return;
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setDescription(description);
        notification.setContent(content);
        notification.setTriggerUser(triggerUser);
        notification.setRelatedItemId(relatedItemId);

        notificationRepository.save(notification);
        log.info("已为用户 '{}' 创建一条新的 '{}' 类型通知。", recipient.getUsername(), type.name());

        // TODO: 未来可以在这里集成WebSocket或推送服务，实现实时通知
    }

    // 获取通知列表，支持按类型筛选
    public PageResponseDto<NotificationDto> getMyNotifications(String currentUserId, String category, PageRequestDto pageRequestDto) {
        log.info("用户ID '{}' 正在获取 '{}' 类别的通知列表...", currentUserId, StringUtils.hasText(category) ? category : "所有");

        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), Sort.by(Sort.Direction.DESC, "createdTime"));

        Page<Notification> notificationPage;

        // 根据前端传入的类别字符串，动态构建查询所需的枚举列表
        if (StringUtils.hasText(category)) {
            List<Notification.NotificationType> typesToQuery = switch (category.toLowerCase()) {
                case "likes" -> List.of(Notification.NotificationType.NEW_POST_LIKE,
                        Notification.NotificationType.NEW_POST_FAVORITE);
                case "comments" -> List.of(Notification.NotificationType.NEW_POST_COMMENT,
                        Notification.NotificationType.NEW_COMMENT_REPLY);
                case "system" -> List.of(Notification.NotificationType.ORDER_CREATED,
                        Notification.NotificationType.ORDER_PAID,
                        Notification.NotificationType.PACKAGE_APPROVED,
                        Notification.NotificationType.PACKAGE_REJECTED);
                default -> throw new IllegalArgumentException("未知的通知类别: " + category);
            };
            notificationPage = notificationRepository.findByRecipientIdAndTypeIn(currentUserId, typesToQuery, pageable);
        } else {
            // 如果不提供类别，则查询所有通知
            notificationPage = notificationRepository.findByRecipientId(currentUserId, pageable);
        }

        return dtoConverter.convertPageToDto(notificationPage, dtoConverter::convertNotificationToDto);
    }

    // 获取未读通知数量
    public UnreadCountDto getUnreadCounts(String currentUserId) {
        log.info("用户ID '{}' 正在获取未读通知数量...", currentUserId);

        List<Object[]> results = notificationRepository.getUnreadCountsGroupedByType(currentUserId);

        Map<String, Long> countsByType = new HashMap<>();
        long totalUnread = 0;

        // 初始化分类计数器
        countsByType.put("LIKES_AND_FAVORITES", 0L);
        countsByType.put("COMMENTS_AND_REPLIES", 0L);
        countsByType.put("SYSTEM_AND_ORDERS", 0L);

        for (Object[] result : results) {
            Notification.NotificationType type = (Notification.NotificationType) result[0];
            long count = (Long) result[1];
            totalUnread += count;

            switch (type) {
                case NEW_POST_LIKE, NEW_POST_FAVORITE -> countsByType.merge("LIKES_AND_FAVORITES", count, Long::sum);
                case NEW_POST_COMMENT, NEW_COMMENT_REPLY ->
                        countsByType.merge("COMMENTS_AND_REPLIES", count, Long::sum);
                default -> countsByType.merge("SYSTEM_AND_ORDERS", count, Long::sum);
            }
        }

        return UnreadCountDto.builder()
                .totalUnread(totalUnread)
                .countsByType(countsByType)
                .build();
    }

    @Transactional
    public void markAsRead(String currentUserId, MarkAsReadRequestDto request) {
        log.info("用户ID '{}' 正在将类型为 {} 的通知标记为已读", currentUserId, request.getTypes());
        if (request.getTypes() == null || request.getTypes().isEmpty()) {
            return;
        }
        notificationRepository.markAsReadByRecipientIdAndTypes(currentUserId, request.getTypes());
    }
}