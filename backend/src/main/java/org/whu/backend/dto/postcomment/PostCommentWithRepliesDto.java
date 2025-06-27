package org.whu.backend.dto.postcomment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.whu.backend.dto.accounts.AuthorDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用于在主列表展示的、带少量预览回复的评论DTO
 */
@Data
@Builder
public class PostCommentWithRepliesDto {
    private String id;
    private String content;
    private AuthorDto author;
    private String replyToUsername;
    private LocalDateTime createdTime;
    @Schema(description = "预览的回复列表（例如最多3条）")
    private List<PostCommentDto> repliesPreview;
    @Schema(description = "这条评论总共有多少条回复")
    private long totalReplies;
}