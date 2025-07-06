package org.whu.backend.service.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.entity.travelpac.PackageComment;
import org.whu.backend.repository.travelRepo.PackageCommentRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 删除一条对旅行团的评论
     * @param commentId 要删除的评论ID
     */
    @Transactional
    public void deleteComment(String commentId) {
        log.info("服务层：开始删除评论ID '{}' 及其所有子评论...", commentId);

        // 1. 查找要删除的根评论
        PackageComment rootComment = packageCommentRepository.findById(commentId)
                .orElseThrow(() -> new BizException("找不到ID为 " + commentId + " 的评论"));

        // 2. 递归查找所有后代评论的ID
        List<String> allCommentIdsToDelete = new ArrayList<>();
        findAllDescendantIds(rootComment, allCommentIdsToDelete);
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

        // 如果是游记评论，也需要类似地更新游记的评论数
        // if (rootComment.getTravelPost() != null) { ... }

        log.info("服务层：评论ID '{}' 及其所有子评论已成功删除。", commentId);
    }

    /**
     * 私有辅助方法：递归查找一个评论的所有后代ID
     * @param parent              父评论
     * @param descendantIds       用于收集后代ID的列表
     */
    private void findAllDescendantIds(PackageComment parent, List<String> descendantIds) {
        // 查找所有直接子评论
        List<PackageComment> children = packageCommentRepository.findByParentId(parent.getId());
        for (PackageComment child : children) {
            descendantIds.add(child.getId());
            // 继续为每个子评论查找它们的后代
            findAllDescendantIds(child, descendantIds);
        }
    }
}