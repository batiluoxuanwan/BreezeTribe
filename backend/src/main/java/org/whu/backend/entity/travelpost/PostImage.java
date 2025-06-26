package org.whu.backend.entity.travelpost;

import jakarta.persistence.*;
import lombok.Data;
import org.whu.backend.entity.MediaFile;

@Data
@Entity
@Table(name = "post_images")
// 这张表是连接 TravelPost 和 MediaFile 的桥梁，并负责排序
public class PostImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false)
    private TravelPost travelPost;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    @Column(nullable = false)
    private int sortOrder; // 图片在这篇游记中的顺序
}