package org.whu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.travelpost.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    // 根据接收者ID和一组通知类型，分页查询通知
    Page<Notification> findByRecipientIdAndTypeIn(String recipientId, List<Notification.NotificationType> types, Pageable pageable);

    // 当不指定类型时，查询所有通知
    Page<Notification> findByRecipientId(String recipientId, Pageable pageable);

    // 将某个用户的所有未读通知标记为已读
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false")
    void markAllAsReadByRecipientId(String recipientId);

    //  按类型分组统计未读数量
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.recipient.id = :recipientId AND n.isRead = false GROUP BY n.type")
    List<Object[]> getUnreadCountsGroupedByType(String recipientId);

    //  将指定类型的通知标记为已读
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.type IN :types AND n.isRead = false")
    void markAsReadByRecipientIdAndTypes(String recipientId, List<Notification.NotificationType> types);
}