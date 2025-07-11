package org.whu.backend.service.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.postcomment.PostCommentCreateRequestDto;
import org.whu.backend.dto.postcomment.PostCommentDto;
import org.whu.backend.dto.postcomment.PostCommentWithRepliesDto;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpost.Comment;
import org.whu.backend.entity.Notification;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.event.ContentSubmissionEvent;
import org.whu.backend.event.post.PostCommentedEvent;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.repository.post.PostCommentRepository;
import org.whu.backend.repository.post.TravelPostRepository;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.NotificationService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserPostCommentService {

    @Autowired
    private PostCommentRepository commentRepository;
    @Autowired
    private TravelPostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DtoConverter dtoConverter;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public static final int MAX_NOTIFICATION_CONTENT_LENGTH = 50;


    /**
     * [已修改] 创建评论，并实现精准的通知推送逻辑
     */
    @Transactional
    public Comment createComment(String postId, PostCommentCreateRequestDto dto, String currentUserId) {
        log.info("用户ID '{}' 正在为游记ID '{}' 发布新评论...", currentUserId, postId);

        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BizException("找不到用户 " + currentUserId));
        TravelPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BizException("找不到游记 " + postId));

        Comment newComment = new Comment();
        newComment.setContent(dto.getContent());
        newComment.setAuthor(author);
        newComment.setTravelPost(post);

        // 如果是回复别人的评论
        if (dto.getParentId() != null && !dto.getParentId().isBlank()) {
            Comment parentComment = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new BizException("找不到要回复的评论 " + dto.getParentId()));

            if (!parentComment.getTravelPost().getId().equals(postId)) {
                throw new BizException("无效操作：不能跨越不同的游记进行回复。");
            }
            newComment.setParent(parentComment);

            // --- 发送“楼中楼回复”通知 ---
            String description = String.format("%s 回复了你的评论", author.getUsername());
            String content = truncateContent(dto.getContent());

            notificationService.createAndSendNotification(
                    parentComment.getAuthor(),           // 接收者：父评论的作者
                    Notification.NotificationType.NEW_COMMENT_REPLY,  // 类型：新的回复
                    description,
                    content,
                    author,                              // 触发者：当前用户
                    post.getId()                         // 关联项目：这篇游记的ID
            );

        } else {
            // --- 发送“一级评论”通知 ---
            eventPublisher.publishEvent(new PostCommentedEvent(post, author, post.getAuthor(), truncateContent(dto.getContent())));
//            String description = String.format("%s 评论了你的游记 %s", author.getUsername(), post.getTitle());
//            String content = truncateContent(dto.getContent());
//
//            notificationService.createAndSendNotification(
//                    post.getAuthor(),                    // 接收者：游记的作者
//                    Notification.NotificationType.NEW_POST_COMMENT,   // 类型：新的评论
//                    description,
//                    content,
//                    author,                              // 触发者：当前用户
//                    post.getId()                         // 关联项目：这篇游记的ID
//            );
        }

        Comment savedComment = commentRepository.save(newComment);
        postRepository.incrementCommentCount(post.getId());
        log.info("评论ID '{}' 已成功发布。", savedComment.getId());

        // 5. 通知AI审核文字
        eventPublisher.publishEvent(new ContentSubmissionEvent(
                this,
                savedComment.getId(),
                ContentSubmissionEvent.ModerationItemType.POST_COMMENT,
                savedComment.getContent()
        ));

        return savedComment;
    }

    /**
     * 删除自己的一条游记评论，如果是一级评论，则直接删除
     * 如果是二级评论且没有人回复，则直接删除
     * 如果是二级评论且被人回复过，则转换为被屏蔽，不减少评论统计量
     * 如果是链条末尾，往上递归解决被屏蔽的评论问题（删除）
     */
    @Transactional
    public void deleteComment(String commentId, String currentUserId) {
        log.info("用户ID '{}' 正在尝试删除游记评论ID '{}'...", currentUserId, commentId);

        // 1. 查找评论并验证所有权
        Comment commentToDelete = commentRepository.findById(commentId)
                .orElseThrow(() -> new BizException("找不到ID为 " + commentId + " 的评论"));

        if (!commentToDelete.getAuthor().getId().equals(currentUserId)) {
            log.error("权限拒绝：用户ID '{}' 试图删除不属于自己的评论ID '{}'。", currentUserId, commentId);
            throw new BizException("无权删除此评论");
        }

        // 2. 分情况讨论
        if (commentToDelete.getParent() == null) {
            // --- 情况A: 这是一级评论，执行“铲掉整栋楼”操作 ---
            handleDeleteTopLevelComment(commentToDelete);
        } else {
            // --- 情况B: 这是楼中楼回复，执行“外科手术” ---
            handleDeleteReplyComment(commentToDelete);
        }
        log.info("评论ID '{}' 的删除操作已完成。", commentId);
    }

    /**
     * 私有方法：处理删除一级评论的逻辑
     */
    private void handleDeleteTopLevelComment(Comment comment) {
        log.info("检测到一级评论删除请求，将删除其所有子孙回复...");

        List<String> descendantIds = commentRepository.findAllDescendantIds(comment.getId());
        Set<String> allIdsToDelete = new HashSet<>(descendantIds);
        allIdsToDelete.add(comment.getId());
        // TODO: 如果允许评论点赞，要移除点赞记录
        // likeRepository.deleteAllByItemIdInAndItemType(allIdsToDelete, InteractionItemType.POST_COMMENT);
        // log.info(" -> 已清除 {} 条关联的点赞记录。", allIdsToDelete.size());

        commentRepository.softDeleteAllByIds(allIdsToDelete);

        long totalDeleted = allIdsToDelete.size();
        postRepository.decrementCommentCount(comment.getTravelPost().getId(), totalDeleted);

        log.info("一级评论ID '{}' 及其 {} 个子孙评论已全部删除。", comment.getId(), descendantIds.size());
    }

    /**
     * 私有方法：处理删除楼中楼回复的逻辑
     */
    private void handleDeleteReplyComment(Comment comment) {
        log.info("检测到楼中楼回复删除请求，ID: {}", comment.getId());

        boolean hasReplies = commentRepository.existsByParentId(comment.getId());

        if (hasReplies) {
            // 如果它下面还有人回复，执行“屏蔽”操作
            log.info(" -> 该评论存在子回复，执行屏蔽操作。");
            // comment.setContent("[该评论已被作者删除]");
            comment.setDeletedByAuthor(true);
            // TODO: 如果允许评论点赞，要移除点赞记录 2
            // likeRepository.deleteAllByItemIdAndItemType(comment.getId(), InteractionItemType.POST_COMMENT);
            commentRepository.save(comment);
        } else {
            // 如果它下面没人回复了，就直接删除，并尝试向上清理
            log.info(" -> 该评论无子回复，执行彻底删除并尝试向上清理。");
            Comment parent = comment.getParent(); // 在删除前获取父评论
            // TODO: 如果允许评论点赞，要移除点赞记录 3
            // likeRepository.deleteAllByItemIdAndItemType(comment.getId(), InteractionItemType.POST_COMMENT);
            commentRepository.delete(comment);
            postRepository.decrementCommentCount(comment.getTravelPost().getId());

            // [关键] 向上递归清理“无后代”的被屏蔽评论
            if (parent != null) {
                cleanupTombstoneChain(parent);
            }
        }
    }

    /**
     * 私有方法：递归向上清理“无后代”的被屏蔽评论
     */
    private void cleanupTombstoneChain(Comment comment) {
        // 检查当前评论是否是一个“无后代的、被屏蔽的”评论
        if (comment.isDeletedByAuthor() && !commentRepository.existsByParentId(comment.getId())) {
            log.info("清理无后代的被屏蔽评论: {}", comment.getId());

            Comment nextParent = comment.getParent();

            // 彻底删除这个无用的占位符
            commentRepository.delete(comment);
            // 注意：减少总评论数，因为它在被屏蔽时没有被减少
            postRepository.decrementCommentCount(comment.getTravelPost().getId());

            // 继续向上检查
            if (nextParent != null) {
                cleanupTombstoneChain(nextParent);
            }
        }
    }


    /**
     * 获取游记的评论列表（带少量预览回复），对游记
     */
    @Transactional(readOnly = true)
    public PageResponseDto<PostCommentWithRepliesDto> getCommentsByPost(String postId, PageRequestDto pageRequestDto) {
        log.info("正在获取游记ID '{}' 的评论列表，分页参数: {}", postId, pageRequestDto);

        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy()));

        Page<Comment> topLevelCommentPage = commentRepository.findByTravelPostIdAndParentIsNull(postId, pageable);

        List<PostCommentWithRepliesDto> dtos = topLevelCommentPage.getContent().stream()
                .map(comment -> {
                    // 为每条一级评论查找它的前3条二级回复作为预览
                    List<PostCommentDto> repliesPreview = commentRepository.findTop3ByParentIdOrderByCreatedTimeAsc(comment.getId())
                            .stream()
                            .map(dtoConverter::convertCommentToDto)
                            .collect(Collectors.toList());

                    long totalReplies = commentRepository.countAllDescendants(comment.getId());

                    return dtoConverter.convertCommentToDtoWithReplies(comment, repliesPreview, totalReplies);
                })
                .collect(Collectors.toList());
//        log.info("查询成功，获取到 {} ");

        return dtoConverter.convertPageToDto(topLevelCommentPage, dtos);
    }

    /**
     * 获取单条评论的所有回复列表（楼中楼详情），对游记
     */
    @Transactional(readOnly = true)
    public PageResponseDto<PostCommentDto> getCommentReplies(String commentId, PageRequestDto pageRequestDto) {
        log.info("正在获取评论ID '{}' 的所有回复...", commentId);

        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(Sort.Direction.ASC, "created_time"));

        // 注意：此处需要数据库支持递归查询。如果不支持，需要用代码模拟递归，会复杂很多。
        Page<Comment> replyPage = commentRepository.findAllRepliesByParentId(commentId, pageable);

        List<PostCommentDto> replyDtos = replyPage.getContent().stream()
                .map(dtoConverter::convertCommentToDto)
                .collect(Collectors.toList());

        return dtoConverter.convertPageToDto(replyPage, replyDtos);
    }

    // 一个私有辅助方法，用于截断过长的内容
    private String truncateContent(String content) {
        if (content.length() <= MAX_NOTIFICATION_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_NOTIFICATION_CONTENT_LENGTH) + "...";
    }
}