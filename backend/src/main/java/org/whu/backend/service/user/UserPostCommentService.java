package org.whu.backend.service.user;

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
import org.whu.backend.dto.postcomment.PostCommentCreateRequestDto;
import org.whu.backend.dto.postcomment.PostCommentDto;
import org.whu.backend.dto.postcomment.PostCommentWithRepliesDto;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpost.Comment;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.repository.post.PostCommentRepository;
import org.whu.backend.repository.post.TravelPostRepository;
import org.whu.backend.service.DtoConverter;

import java.util.List;
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

    /**
     * 发布一条新评论或回复，对游记
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

            // [安全漏洞修复] 关键！验证父评论是否属于当前帖子
            if (!parentComment.getTravelPost().getId().equals(postId)) {
                log.warn("用户 '{}' 尝试跨帖子回复！帖子ID: '{}', 父评论所属帖子ID: '{}'",
                        currentUserId, postId, parentComment.getTravelPost().getId());
                throw new BizException("无效操作：不能跨越不同的游记进行回复。");
            }
            newComment.setParent(parentComment);
        }

        Comment savedComment = commentRepository.save(newComment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        log.info("评论ID '{}' 已成功发布。", savedComment.getId());
        return savedComment;
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

                    long totalReplies = commentRepository.countByParentId(comment.getId());

                    return dtoConverter.convertCommentToDtoWithReplies(comment, repliesPreview, totalReplies);
                })
                .collect(Collectors.toList());
//        log.info("查询成功，获取到 {} ");

        return dtoConverter.convertPageToDto(topLevelCommentPage,dtos);
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

        return dtoConverter.convertPageToDto(replyPage,replyDtos);
    }
}