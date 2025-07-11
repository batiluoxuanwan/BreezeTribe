package org.whu.backend.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.postcomment.PostCommentCreateRequestDto;
import org.whu.backend.dto.postcomment.PostCommentDto;
import org.whu.backend.entity.travelpost.Comment;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.user.UserPostCommentService;
import org.whu.backend.util.AccountUtil;

@Tag(name = "用户-游记评论功能", description = "用户对游记进行评论和回复")
@RestController
@Slf4j
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/user/posts/comments")
public class UserPostCommentController {

    @Autowired
    private UserPostCommentService userCommentService;
    @Autowired
    private DtoConverter dtoConverter;


    @Operation(summary = "发布一条新的对游记的评论或回复",description = "可以是对游记，parentId就填null或者空串，可以是对游记里的评论盖楼中楼")
    @PostMapping("/{postId}")
    public Result<PostCommentDto> createComment(
            @PathVariable String postId,
            @Valid @RequestBody PostCommentCreateRequestDto createRequestDto
    ) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问发布评论接口", currentUserId);

        Comment savedComment = userCommentService.createComment(postId, createRequestDto, currentUserId);

        return Result.success(dtoConverter.convertCommentToDto(savedComment));
    }

    @Operation(summary = "删除自己发布的对游记的评论")
    @DeleteMapping("/{commentId}")
    public Result<?> deleteComment(@PathVariable String commentId) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问删除游记评论接口, Comment ID: {}", currentUserId, commentId);

        userCommentService.deleteComment(commentId, currentUserId);

        return Result.success("评论删除成功");
    }
}