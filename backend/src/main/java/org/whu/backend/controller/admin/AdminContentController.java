package org.whu.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    @Operation(summary = "删除一条对旅行团评论", description = "管理员强制删除一条违规或不当的评论。")
    @DeleteMapping("/package-comments/{commentId}")
    public Result<?> deleteComment(
            @Parameter(description = "要删除的评论ID") @PathVariable String commentId) {
        log.info("请求日志：管理员正在删除评论ID '{}'", commentId);
        adminContentService.deleteComment(commentId);
        return Result.success("评论删除成功");
    }

    // 未来可以增加删除游记的接口
    /*
    @DeleteMapping("/posts/{postId}")
    public Result<?> deletePost(@PathVariable String postId) {
        // ...
    }
    */
}