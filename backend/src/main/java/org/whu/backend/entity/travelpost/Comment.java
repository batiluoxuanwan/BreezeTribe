package org.whu.backend.entity.travelpost;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.whu.backend.entity.accounts.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@SoftDelete
@Table(name = "post_comments")
// 用于实现游记的评论功能
public class Comment {
    @Id
    @Column(length = 36)
    private String id;

    @Lob
    @Column(nullable = false)
    private String content; // 评论内容

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author; // 评论者

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false)
    private TravelPost travelPost; // 评论的是哪篇游记

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id") // 用于实现楼中楼回复
    private Comment parent;

    // 一个专门用于作者主动删除的标记。默认为false。当作者删除一条有子回复的评论时，此标记会变为true。
    @ColumnDefault("false")
    private boolean isDeletedByAuthor = false;

    // 让手写的sql语句认识这个字段
    @Column(nullable = false, insertable = false, updatable = false)
    private boolean deleted = false;

    @CreationTimestamp
    private LocalDateTime createdTime;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}