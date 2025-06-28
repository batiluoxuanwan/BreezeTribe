package org.whu.backend.entity;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.whu.backend.entity.accounts.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 每一个实体都对应一个被点赞的东西和收藏的账号
 */
@Data
@Entity
@Table(name = "likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_account_id", "item_id", "item_type"})) // 一个人对同一个东西只能点赞一次
public class Like {
    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LikeItemType itemType; // 点赞的项目的类型

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    // 表示收藏的所属账号
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_account_id", referencedColumnName = "id",nullable = false)
    private User user;

    // 收藏的项目的ID
    @Column(length = 36, nullable = false)
    private String itemId;

    public enum LikeItemType {
        POST     // 游记
    }

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}