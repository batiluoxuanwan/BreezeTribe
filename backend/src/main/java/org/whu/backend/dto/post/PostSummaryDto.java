package org.whu.backend.dto.post;

import lombok.Builder;
import lombok.Data;
import org.whu.backend.dto.accounts.AuthorDto;
import org.whu.backend.dto.spot.SpotDetailDto;

import java.time.LocalDateTime;

@Data
@Builder
public class PostSummaryDto {
    private String id;
    private String title;
    private String coverImageUrl; // 游记的第一张图作为封面
    private AuthorDto author;
    private SpotDetailDto spot;
    private int likeCount;
    private int favoriteCount;
    private int commentCount;
    private LocalDateTime createdTime;
}