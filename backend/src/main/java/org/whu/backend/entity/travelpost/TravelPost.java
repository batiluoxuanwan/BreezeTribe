package org.whu.backend.entity.travelpost;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.whu.backend.entity.Spot;
import org.whu.backend.entity.accounts.User;

import java.time.LocalDateTime;
import java.util.*;

// 游记实体，模仿小红书的笔记模式
@Data
@Entity
@SoftDelete
@Table(name = "travel_posts")
public class TravelPost {

    @Id
    @Column(length = 36)
    private String id;

    // 游记标题
    @Column(nullable = false)
    private String title;

    // 游记的正文内容，使用@Lob来存储长文本
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 作者：这篇游记是谁写的 (多对一关联到User实体)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // 关联的地点/景点 (多对一关联到Spot实体)，这个字段是可选的，一篇游记可以不关联任何具体地点
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "spot_id")
    private Spot spot;

    // --- 互动数据计数器 ---
    @ColumnDefault("0")
    private Integer viewCount = 0; // 浏览量

    @ColumnDefault("0")
    private Integer likeCount = 0; // 点赞量

    @ColumnDefault("0")
    private Integer favoriteCount = 0; // 收藏量

    @ColumnDefault("0")
    private Integer commentCount = 0; // 评论量

    // --- 关联关系 ---

    /**
     * 图集：这篇游记包含了哪些图片 (一对多关联到PostImage中间表)
     * 通过这个中间表，实现了图片复用和排序
     */
    @OneToMany(mappedBy = "travelPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PostImage> images = new ArrayList<>();

    /**
     * 标签：这篇游记打了哪些标签 (多对多关联到Tag实体)
     * 用于分类和搜索
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    // --- 时间戳与软删除 ---

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