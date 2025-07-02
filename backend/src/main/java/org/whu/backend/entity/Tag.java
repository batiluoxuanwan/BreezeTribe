package org.whu.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@SoftDelete
@Table(name = "tags")
public class Tag {
    @Id
    @Column(length = 36)
    private String id;

    @Column(unique = true, nullable = false, length = 50)
    private String name; // 标签名，如 "巴黎铁塔"、"美食探店"

    // 标签分类，使用枚举类型确保分类的规范性。
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagCategory category;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime; // 创建时间

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTime; // 更新时间

    /**
     * 标签分类的枚举
     */
    public enum TagCategory {
        THEME,          // 主题特色 (如: 古镇风情, 登山徒步)
        TARGET_AUDIENCE,// 适合人群 (如: 亲子同游, 情侣蜜月)
        DESTINATION,    // 目的地/地理位置 (如: 云南, 日本)
        FEATURE         // 其他特色 (如: 性价比高, 美食之旅)
    }


    @PrePersist
    protected void onPrePersist() {
        if (this.id == null || this.id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
    }
}