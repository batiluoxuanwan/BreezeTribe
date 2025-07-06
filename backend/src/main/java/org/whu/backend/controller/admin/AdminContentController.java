package org.whu.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.service.admin.AdminContentService;

@Tag(name = "管理员-内容管理", description = "提供对平台用户生成内容（如评论、游记）进行管理的接口")
@RestController
@Slf4j
@RequestMapping("/api/admin/content")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {

    @Autowired
    private AdminContentService adminContentService;

    // --- 删除操作 ---
    @Operation(summary = "删除一篇【游记】", description = "管理员强制删除一篇违规或不当的游记，会一并删除其所有评论、点赞等。")
    @DeleteMapping("/posts/{postId}")
    public Result<?> deleteTravelPost(@Parameter(description = "要删除的游记ID") @PathVariable String postId) {
        log.info("请求日志：管理员正在删除游记ID '{}'", postId);
        adminContentService.deleteTravelPost(postId);
        return Result.success("游记删除成功");
    }

    @Operation(summary = "删除一条对【游记】的评论", description = "管理员强制删除，会一并删除其所有楼中楼回复。")
    @DeleteMapping("/post-comments/{commentId}")
    public Result<?> deletePostComment(@Parameter(description = "要删除的游记评论ID") @PathVariable String commentId) {
        log.info("请求日志：管理员正在删除游记评论ID '{}'", commentId);
        adminContentService.deletePostComment(commentId);
        return Result.success("游记评论删除成功");
    }

    @Operation(summary = "删除一条对【旅行团】的评论", description = "管理员强制删除，会一并删除其所有楼中楼回复。")
    @DeleteMapping("/package-comments/{commentId}")
    public Result<?> deletePackageComment(@Parameter(description = "要删除的旅行团评论ID") @PathVariable String commentId) {
        log.info("请求日志：管理员正在删除旅行团评论ID '{}'", commentId);
        adminContentService.deletePackageComment(commentId);
        return Result.success("旅行团评论删除成功");
    }

    // --- 屏蔽操作 ---
    @Operation(summary = "屏蔽一个【旅行团】", description = "将旅行团状态设置为REJECTED，对用户不可见。")
    @PutMapping("/packages/{packageId}/block")
    public Result<?> blockTravelPackage(@Parameter(description = "要屏蔽的旅行团ID") @PathVariable String packageId) {
        log.info("请求日志：管理员正在屏蔽旅行团ID '{}'", packageId);
        adminContentService.blockTravelPackage(packageId);
        return Result.success("旅行团屏蔽成功");
    }

}