package org.whu.backend.entity.travelpost;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.whu.backend.entity.accounts.Account;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * [新增] 通知实体
 */
@Data
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(length = 36)
    private String id;

    /**
     * 通知接收者
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Account recipient;

    /**
     * 是否已读
     */
    @ColumnDefault("false")
    private boolean isRead = false;

    /**
     * 通知类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * 通知描述，例如："张三 回复了你的评论"
     */
    @Column(nullable = true)
    private String description;

    /**
     * 通知内容，例如："评论内容：哈哈哈哈"
     */
    @Column(nullable = false)
    private String content;

    /**
     * 触发此通知的用户 (可选，系统通知可能没有触发者)
     * 例如：点赞你的人，评论你的人
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trigger_user_id")
    private Account triggerUser;

    /**
     * 关联项目的ID (例如 游记ID, 订单ID)
     * 用于点击通知后跳转
     */
    @Column(length = 36)
    private String relatedItemId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }

    public enum NotificationType {
        // 点赞和收藏类
        NEW_POST_LIKE,          // 游记被点赞
        NEW_POST_FAVORITE,      // 游记被收藏

        // 评论和回复类
        NEW_POST_COMMENT,       // 游记被评论
        NEW_COMMENT_REPLY,      // 你的评论被回复

        // 系统和订单类
        ORDER_CREATED,          // 订单创建成功
        ORDER_PAID,             // 订单支付成功
        PACKAGE_APPROVED,       // 旅行团审核通过
        PACKAGE_REJECTED        // 旅行团审核被驳回
    }
}