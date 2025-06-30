package org.whu.backend.controller.user;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.packagecomment.PackageCommentCreateDto;
import org.whu.backend.dto.packagecomment.PackageCommentDto;
import org.whu.backend.entity.PackageComment;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.user.UserPackageCommentService;
import org.whu.backend.util.AccountUtil;

@Tag(name = "用户-旅游团评论功能", description = "用户对旅游团进行评论和回复")
@RestController
@Slf4j
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/user/packages/comments")
public class UserPackageCommentController {
    private final UserPackageCommentService userPackageCommentService;
    private final DtoConverter dtoConverter;

    public UserPackageCommentController(UserPackageCommentService userPackageCommentService, DtoConverter dtoConverter) {
        this.userPackageCommentService = userPackageCommentService;
        this.dtoConverter = dtoConverter;
    }

    @Operation(summary = "发布对旅行团的新评价或回复")
    @PostMapping("/{packageId}")
    public Result<PackageCommentDto> createPackageComment(
            @PathVariable String packageId,
            @Valid @RequestBody PackageCommentCreateDto createRequestDto
    ) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 正在为旅行团 '{}' 发布评价", currentUserId, packageId);

        PackageComment savedComment = userPackageCommentService.createComment(packageId, createRequestDto, currentUserId);

        // 调用转换服务将 savedComment 转换为DTO
        PackageCommentDto dto = dtoConverter.convertPackageCommentToSimpleDto(savedComment);

        return Result.success("发布成功", dto);
    }

    // 删除自己发布的对旅行团的评价
    @Operation(summary = "删除自己发布的对旅行团的评价")
    @DeleteMapping("/package-comments/{commentId}")
    public Result<?> deletePackageComment(@PathVariable String commentId) {
        String currentUserId = AccountUtil.getCurrentAccountId();
        log.info("用户ID '{}' 访问删除旅行团评价接口, Comment ID: {}", currentUserId, commentId);

        userPackageCommentService.deleteComment(commentId, currentUserId);

        return Result.success("评价删除成功");
    }
}
