package org.whu.backend.dto.post;


import lombok.Builder;
import lombok.Data;
import org.whu.backend.dto.accounts.AuthorDto;
import org.whu.backend.dto.spot.SpotDetailDto;
import org.whu.backend.dto.tag.TagDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class PostDetailToOwnerDto {
    private String id;
    private String title;
    private String content;
    private AuthorDto author;
    private SpotDetailDto spot; // 关联的景点摘要信息
    private Map<String, String> imageIdAndUrls; // 返回带签名的图片Id与URL列表
    private int likeCount;
    private int favoriteCount;
    private int commentCount;
    private int viewCount;
    private List<TagDto> tags;
    private LocalDateTime createdTime;
}