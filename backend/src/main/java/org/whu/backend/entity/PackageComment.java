package org.whu.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.whu.backend.entity.accounts.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * [新增] 对旅行团的评价实体
 */
@Data
@Entity
@SoftDelete
@Table(name = "package_comments")
public class PackageComment {
    @Id
    @Column(length = 36)
    private String id;

    /**
     * 评价星级 (1-5星)
     */
    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @Lob
    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "package_id", nullable = false)
    private TravelPackage travelPackage;

    /**
     * 楼中楼回复的父评论
     * V1版本我们只支持一层回复，所以这个字段只会被一级评论的回复所使用
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private PackageComment parent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}