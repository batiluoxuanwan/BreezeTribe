package org.whu.backend.dto.packagecomment;

import lombok.Builder;
import lombok.Data;
import org.whu.backend.dto.accounts.AuthorDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用于展示旅行团评价的DTO (带回复预览)
 */
@Data
@Builder
public class PackageCommentDto {
    private String id;
    private Integer rating;
    private String content;
    private AuthorDto author;
    private String replyToUsername;
    private LocalDateTime createdTime;
    private List<PackageCommentDto> repliesPreview; // 回复预览
    private long totalReplies; // 回复总数
}