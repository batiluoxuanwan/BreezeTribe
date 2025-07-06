package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.whu.backend.entity.accounts.Account;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 举报实体，用于记录用户的举报信息
 */
@Data
@Entity
@Table(name = "reports")
public class Report {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String reportedItemId; // 被举报内容（游记、评论等）的ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportedItemType itemType; // 被举报内容的类型

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason; // 举报原因

    @Lob
    @Column(columnDefinition = "TEXT")
    private String additionalInfo; // 用户补充的额外信息

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary; // 被举报内容的摘要


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING; // 举报处理状态

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Account reporter; // 举报人

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }

    /**
     * 被举报内容的类型枚举
     */
    public enum ReportedItemType {
        TRAVEL_PACKAGE,
        TRAVEL_POST,
        PACKAGE_COMMENT,
        POST_COMMENT
    }

    /**
     * 举报原因枚举
     */
    public enum ReportReason {
        SPAM_AD,            // 垃圾广告
        PORNOGRAPHIC,       // 色情内容
        ILLEGAL_CONTENT,    // 违法内容
        PERSONAL_ATTACK,    // 人身攻击
        OTHER               // 其他原因
    }

    /**
     * 举报处理状态枚举
     */
    public enum ReportStatus {
        PENDING, // 待处理
        ACCEPTED, // 已受理（并已处理违规内容）
        REJECTED  // 已驳回（举报不成立）
    }
}