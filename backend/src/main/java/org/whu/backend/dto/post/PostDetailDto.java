package org.whu.backend.dto.post;

import lombok.Builder;
import lombok.Data;
import org.whu.backend.dto.accounts.AuthorDto;
import org.whu.backend.dto.spot.SpotDetailDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostDetailDto {
    private String id;
    private String title;
    private String content;
    private AuthorDto author;
    private SpotDetailDto spot; // 关联的景点摘要信息
    private List<String> imageUrls; // 返回带签名的图片URL列表
    private int likeCount;
    private int favoriteCount;
    private int commentCount;
    private int viewCount;
    private LocalDateTime createdTime;
}