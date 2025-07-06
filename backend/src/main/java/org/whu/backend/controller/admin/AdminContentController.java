package org.whu.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.report.ReportDto;
import org.whu.backend.entity.Report;
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
    @Operation(summary = "删除一篇【游记】", description = "管理员强制删除一篇违规或不当的游记，会一并删除其所有评论、点赞等。" +
            "同时会给被删游记的人发一条类型为POST_DELETED的系统通知，没有相关联的实体id")
    @DeleteMapping("/posts/{postId}")
    public Result<?> deleteTravelPost(@Parameter(description = "要删除的游记ID") @PathVariable String postId) {
        log.info("请求日志：管理员正在删除游记ID '{}'", postId);
        adminContentService.deleteTravelPost(postId);
        return Result.success("游记删除成功");
    }

    @Operation(summary = "删除一条对【游记】的评论", description = "管理员强制删除，会一并删除其所有楼中楼回复。同时会给被删除评论的用户发送一条系统通知，" +
            "通知的类型为POST_COMMENT_DELETED，相关的实体为评论对应的游记id")
    @DeleteMapping("/post-comments/{commentId}")
    public Result<?> deletePostComment(@Parameter(description = "要删除的游记评论ID") @PathVariable String commentId) {
        log.info("请求日志：管理员正在删除游记评论ID '{}'", commentId);
        adminContentService.deletePostComment(commentId);
        return Result.success("游记评论删除成功");
    }

    @Operation(summary = "删除一条对【旅行团】的评论", description = "管理员强制删除，会一并删除其所有楼中楼回复。同时会给被删除评论的用户发送一条系统通知，" +
            "通知的类型为PACKAGE_COMMENT_DELETED，相关的实体为评论对应的旅行团id")
    @DeleteMapping("/package-comments/{commentId}")
    public Result<?> deletePackageComment(@Parameter(description = "要删除的旅行团评论ID") @PathVariable String commentId) {
        log.info("请求日志：管理员正在删除旅行团评论ID '{}'", commentId);
        adminContentService.deletePackageComment(commentId);
        return Result.success("旅行团评论删除成功");
    }

    // --- 屏蔽操作 ---
    @Operation(summary = "屏蔽一个【旅行团】", description = "将旅行团状态设置为REJECTED，对用户不可见。" +
            "同时会给经销商发送一条类型为PACKAGE_BLOCKED通知，相关实体为被屏蔽的旅游团id")
    @PutMapping("/packages/{packageId}/block")
    public Result<?> blockTravelPackage(@Parameter(description = "要屏蔽的旅行团ID") @PathVariable String packageId) {
        log.info("请求日志：管理员正在屏蔽旅行团ID '{}'", packageId);
        adminContentService.blockTravelPackage(packageId);
        return Result.success("旅行团屏蔽成功");
    }

    // --- 举报受理与驳回操作 ----
    @Operation(summary = "获取举报列表（分页）", description = """
            可以按状态筛选，留空则查询全部。-----\
            举报类型：TRAVEL_PACKAGE(旅行团)/TRAVEL_POST(游记)/PACKAGE_COMMENT(旅行团评论)/POST_COMMENT(游记评论)---------
            举报原因：SPAM_AD(垃圾广告)/PORNOGRAPHIC(色情内容)/ILLEGAL_CONTENT(违法内容)/PERSONAL_ATTACK(人身攻击)/OTHER(其他)--------
            处理状态：PENDING(待处理)/ACCEPTED(已受理)/REJECTED(已驳回)""")
    @GetMapping
    public Result<PageResponseDto<ReportDto>> getReports(
            @Parameter(description = "按状态筛选（可选），如 PENDING, ACCEPTED, REJECTED")
            @RequestParam(required = false) Report.ReportStatus status,
            @Valid @ParameterObject PageRequestDto pageRequestDto) {
        PageResponseDto<ReportDto> reports = adminContentService.getReportsByStatus(status, pageRequestDto);
        return Result.success("查询成功", reports);
    }

    @Operation(summary = "受理一个举报", description = "将举报标记为已处理，并删除相应的违规内容，同时给举报用户发送举报已受理通知。" +
            "通知的类型为REPORT_ACCEPTED，关联的实体为举报对象的id（不过意义好像不大）")
    @PostMapping("/reports/{reportId}/accept")
    public Result<?> acceptReport(@PathVariable String reportId) {
        adminContentService.acceptReport(reportId);
        return Result.success("举报已受理");
    }

    @Operation(summary = "驳回一个举报", description = "将举报标记为不成立，同时给举报用户发送举报被驳回的通知。" +
            "通知的类型为REPORT_REJECTED，关联的实体为举报对象的id，因为无法判断是什么类型，所以会在通知的content部分返回举报对象的类型" +
            "( TRAVEL_PACKAGE, TRAVEL_POST, PACKAGE_COMMENT, POST_COMMENT )")
    @PostMapping("/reports/{reportId}/reject")
    public Result<?> rejectReport(@PathVariable String reportId) {
        adminContentService.rejectReport(reportId);
        return Result.success("举报已驳回");
    }

}