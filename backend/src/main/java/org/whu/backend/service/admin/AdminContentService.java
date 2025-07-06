package org.whu.backend.service.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.report.ReportDto;
import org.whu.backend.entity.InteractionItemType;
import org.whu.backend.entity.Notification;
import org.whu.backend.entity.Report;
import org.whu.backend.entity.travelpac.PackageComment;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.entity.travelpost.Comment;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.repository.FavoriteRepository;
import org.whu.backend.repository.LikeRepository;
import org.whu.backend.repository.ReportRepository;
import org.whu.backend.repository.post.PostCommentRepository;
import org.whu.backend.repository.post.TravelPostRepository;
import org.whu.backend.repository.travelRepo.PackageCommentRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.NotificationService;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.truncate;

/**
 * 管理员内容管理服务
 */
@Service
@Slf4j
public class AdminContentService {

    @Autowired
    private PackageCommentRepository packageCommentRepository;
    @Autowired
    private TravelPackageRepository travelPackageRepository;
    @Autowired
    private PostCommentRepository postCommentRepository;
    @Autowired
    private TravelPostRepository travelPostRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private DtoConverter dtoConverter;

    /**
     * 删除一条对旅行团的评论
     *
     * @param commentId 要删除的评论ID
     */
    @Transactional
    public void deletePackageComment(String commentId) {
        log.info("服务层：开始删除旅行团评论ID '{}' 及其所有子评论...", commentId);

        // 1. 查找要删除的根评论
        PackageComment rootComment = packageCommentRepository.findById(commentId)
                .orElseThrow(() -> new BizException("找不到ID为 " + commentId + " 的评论"));

        // 2. 递归查找所有后代评论的ID
        List<String> allCommentIdsToDelete = new ArrayList<>();
        findAllPackageCommentDescendantIds(rootComment, allCommentIdsToDelete);
        allCommentIdsToDelete.add(rootComment.getId()); // 把根评论自己也加进去

        int deleteCount = allCommentIdsToDelete.size();
        log.info("服务层：将要删除 {} 条评论（包括根评论和所有子评论）。", deleteCount);

        // 3. 批量删除所有相关评论
        packageCommentRepository.deleteAllByIdInBatch(allCommentIdsToDelete);

        // 4. 更新所属旅行团的评论总数
        if (rootComment.getTravelPackage() != null) {
            String packageId = rootComment.getTravelPackage().getId();
            // 使用原子操作减去评论数
            travelPackageRepository.decrementCommentCount(packageId, deleteCount);
            log.info("服务层：产品ID '{}' 的评论总数已减少 {}", packageId, deleteCount);
        }

        // 更新旅行团的评论数
        if (rootComment.getTravelPackage() != null) {
            travelPackageRepository.decrementCommentCount(rootComment.getTravelPackage().getId(), allCommentIdsToDelete.size());
        }

        // 发送通知
        if (rootComment.getTravelPackage() != null) {
            String description = String.format("您在旅行团 [%s] 下的评论涉嫌违规，已被管理员移除，请您遵守社区规范，文明发言。", rootComment.getTravelPackage().getTitle());
            notificationService.createAndSendNotification(
                    rootComment.getAuthor(),
                    Notification.NotificationType.PACKAGE_COMMENT_DELETED,
                    description,
                    null,
                    null,
                    rootComment.getTravelPackage().getId()
            );
        }
        log.info("服务层：评论ID '{}' 及其所有子评论已成功删除。", commentId);
    }

    /**
     * 删除一条游记评论及其所有后代评论
     */
    @Transactional
    public void deletePostComment(String commentId) {
        log.info("服务层：开始删除游记评论ID '{}' 及其所有子评论...", commentId);
        Comment rootComment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new BizException("找不到ID为 " + commentId + " 的游记评论"));

        List<String> allCommentIdsToDelete = new ArrayList<>();
        findAllPostCommentDescendantIds(rootComment, allCommentIdsToDelete);
        allCommentIdsToDelete.add(rootComment.getId());

        postCommentRepository.deleteAllByIdInBatch(allCommentIdsToDelete);

        if (rootComment.getTravelPost() != null) {
            travelPostRepository.decrementCommentCount(rootComment.getTravelPost().getId(), allCommentIdsToDelete.size());
        }

        // 发送通知
        if (rootComment.getTravelPost() != null) {
            String description = String.format("您在游记 [%s] 下的评论涉嫌违规，已被管理员移除，请您遵守社区规范，文明发言。", rootComment.getTravelPost().getTitle());
            notificationService.createAndSendNotification(
                    rootComment.getAuthor(),
                    Notification.NotificationType.POST_COMMENT_DELETED,
                    description,
                    null,
                    null,
                    rootComment.getTravelPost().getId()
            );
        }
        log.info("服务层：游记评论ID '{}' 及其 {} 条子评论已成功删除。", commentId, allCommentIdsToDelete.size() - 1);
    }

    /**
     * 删除一篇游记，并级联删除其所有评论
     */
    @Transactional
    public void deleteTravelPost(String postId) {
        log.info("服务层：开始删除游记ID '{}'及其所有关联数据...", postId);
        TravelPost travelPost = travelPostRepository.findById(postId)
                .orElseThrow(() -> new BizException("找不到ID为 " + postId + " 的游记"));
        // 1. 删除所有关联的评论
        long deletedComments = postCommentRepository.deleteByTravelPostId(postId);
        log.info("服务层：删除了 {} 条与该游记关联的评论。", deletedComments);

        // 2. 删除所有关联的点赞和收藏
        likeRepository.deleteAllByItemIdAndItemType(postId, InteractionItemType.POST);
        favoriteRepository.deleteAllByItemIdAndItemType(postId, InteractionItemType.POST);

        // 3. 最后删除游记本身
        travelPostRepository.deleteById(postId);


        String description = String.format("您的游记 [%s] 涉嫌违规，已被管理员移除，请您遵守社区规范，文明发言。", travelPost.getTitle());
        notificationService.createAndSendNotification(
                travelPost.getAuthor(),
                Notification.NotificationType.POST_DELETED,
                description,
                null,
                null,
                null
        );
        log.info("服务层：游记ID '{}' 已成功删除。", postId);
    }

    /**
     * 屏蔽一个旅游团，仅屏蔽
     */
    @Transactional
    public void blockTravelPackage(String packageId) {
        TravelPackage pkg = travelPackageRepository.findById(packageId).orElseThrow(() -> new BizException("找不到该旅行团"));
        pkg.setStatus(TravelPackage.PackageStatus.REJECTED);
        travelPackageRepository.save(pkg);
        String description = String.format("您的旅游团 [%s] 涉嫌违规，已被管理员屏蔽，请您编辑合规后再次申请上架。", pkg.getTitle());
        notificationService.createAndSendNotification(
                pkg.getDealer(),
                Notification.NotificationType.PACKAGE_BLOCKED,
                description,
                null,
                null,
                packageId
        );
        log.info("服务层：管理员已屏蔽旅行团ID '{}'", packageId);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<ReportDto> getReportsByStatus(Report.ReportStatus status, PageRequestDto pageRequestDto) {
        log.info("服务层：开始查询状态为 '{}' 的举报列表...", status);
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(Sort.Direction.DESC, pageRequestDto.getSortBy()));

        Page<Report> reportPage;
        if (status != null) {
            reportPage = reportRepository.findByStatus(status, pageable);
        } else {
            // 如果不提供状态，默认查询全部
            reportPage = reportRepository.findAll(pageable);
        }
        return dtoConverter.convertPageToDto(reportPage, dtoConverter::convertReportToDto);
    }

    @Transactional
    public void acceptReport(String reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new BizException("找不到该举报"));
        if (report.getStatus() != Report.ReportStatus.PENDING) {
            throw new BizException("该举报已被处理");
        }

        String description = "您好，您提交的举报已被管理员受理，相关内容已处理。感谢您为维护社区环境做出的贡献！";
        String itemTitle = "相关内容"; // 默认标题

        // 根据举报类型，调用相应的删除方法，并获取内容标题用于通知
        switch (report.getItemType()) {
            case TRAVEL_POST -> {
                itemTitle = travelPostRepository.findById(report.getReportedItemId()).map(TravelPost::getTitle).orElse("一篇已删除的游记");
                deleteTravelPost(report.getReportedItemId());
                description = String.format("您对游记 [%s] 的举报已被受理，相关内容已处理。感谢您为维护社区环境做出的贡献！", itemTitle);
            }
            case POST_COMMENT -> {
                Comment comment = postCommentRepository.findById(report.getReportedItemId()).orElse(null);
                if(comment != null) itemTitle = comment.getContent();
                deletePostComment(report.getReportedItemId());
                description = String.format("您对一条游记评论（内容：“%s...”）的举报已被受理，感谢您为维护社区环境做出的贡献！", truncate(itemTitle, 15));
            }
            case PACKAGE_COMMENT -> {
                PackageComment comment = packageCommentRepository.findById(report.getReportedItemId()).orElse(null);
                if(comment != null) itemTitle = comment.getContent();
                deletePackageComment(report.getReportedItemId());
                description = String.format("您对一条旅游团评论（内容：“%s...”）的举报已被受理，感谢您为维护社区环境做出的贡献！", truncate(itemTitle, 15));
            }
            case TRAVEL_PACKAGE -> {
                itemTitle = travelPackageRepository.findById(report.getReportedItemId()).map(TravelPackage::getTitle).orElse("一个已下架的产品");
                blockTravelPackage(report.getReportedItemId());
                description = String.format("您对旅行团 [%s] 的举报已被受理，相关内容已处理。感谢您为维护社区环境做出的贡献！", itemTitle);
            }
        }

        report.setStatus(Report.ReportStatus.ACCEPTED);
        reportRepository.save(report);

        // 发送通知给举报人
        notificationService.createAndSendNotification(
                report.getReporter(),
                Notification.NotificationType.REPORT_ACCEPTED,
                description,
                null,
                null,
                report.getReportedItemId()
        );

        log.info("服务层：已受理举报ID '{}'，并处理了相关内容。", reportId);
    }

    @Transactional
    public void rejectReport(String reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new BizException("找不到该举报"));
        if (report.getStatus() != Report.ReportStatus.PENDING) {
            throw new BizException("该举报已被处理");
        }
        report.setStatus(Report.ReportStatus.REJECTED);
        reportRepository.save(report);

        // 发送通知给举报人
        String description = String.format("经核实，您举报的内容 [%s] 暂未发现明显违规。感谢您对社区的关注与支持！",report.getSummary());
        notificationService.createAndSendNotification(
                report.getReporter(),
                Notification.NotificationType.REPORT_REJECTED,
                description,
                report.getItemType().toString(),
                null,
                report.getReportedItemId()
        );

        log.info("服务层：已驳回举报ID '{}'。", reportId);
    }


    /**
     * 私有辅助方法：递归查找一个旅行团评论的所有后代ID
     *
     * @param parent        父评论
     * @param descendantIds 用于收集后代ID的列表
     */
    private void findAllPackageCommentDescendantIds(PackageComment parent, List<String> descendantIds) {
        List<PackageComment> children = packageCommentRepository.findByParentId(parent.getId());
        for (PackageComment child : children) {
            descendantIds.add(child.getId());
            findAllPackageCommentDescendantIds(child, descendantIds);
        }
    }

    private void findAllPostCommentDescendantIds(Comment parent, List<String> descendantIds) {
        List<Comment> children = postCommentRepository.findByParentId(parent.getId());
        for (Comment child : children) {
            descendantIds.add(child.getId());
            findAllPostCommentDescendantIds(child, descendantIds);
        }
    }
}