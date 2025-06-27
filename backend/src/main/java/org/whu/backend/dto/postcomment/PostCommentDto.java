package org.whu.backend.dto.postcomment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.whu.backend.dto.accounts.AuthorDto;

import java.time.LocalDateTime;

/**
 * 用于在楼中楼详情页或简单嵌套中展示的评论DTO
 */
@Data
@Builder
public class PostCommentDto {
    private String id;
    private String content;
    private AuthorDto author;
    @Schema(description = "被回复的评论ID，如果是对游记的，则为空或者null")
    private String parentId;
    @Schema(description = "被回复人的用户名")
    private String replyToUsername;
    @Schema(description = "被回复人的id")
    private String replyToUserId;
    private LocalDateTime createdTime;
}