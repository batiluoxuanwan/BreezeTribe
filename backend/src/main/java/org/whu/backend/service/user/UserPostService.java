package org.whu.backend.service.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.post.*;
import org.whu.backend.entity.InteractionItemType;
import org.whu.backend.entity.MediaFile;
import org.whu.backend.entity.Spot;
import org.whu.backend.entity.Tag;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpost.PostImage;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.event.ContentSubmissionEvent;
import org.whu.backend.repository.FavoriteRepository;
import org.whu.backend.repository.LikeRepository;
import org.whu.backend.repository.MediaFileRepository;
import org.whu.backend.repository.TagRepository;
import org.whu.backend.repository.post.PostCommentRepository;
import org.whu.backend.repository.post.TravelPostRepository;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.merchant.MerchantPackageService;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserPostService {
    @Autowired
    private TravelPostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MediaFileRepository mediaFileRepository;
    @Autowired
    private MerchantPackageService merchantPackageService;
    @Autowired
    private DtoConverter dtoConverter;
    @Autowired
    private PostCommentRepository postCommentRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private TravelPostRepository travelPostRepository;
    @Autowired
    private ApplicationEventPublisher eventPublisher; // 注入事件发布器

    @Transactional
    public TravelPost createPost(PostCreateRequestDto dto, String currentUserId) {
        log.info("用户ID '{}' 正在创建新游记, 标题: '{}'", currentUserId, dto.getTitle());

        // 1. 查找作者实体
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BizException("找不到ID为 " + currentUserId + " 的用户"));

        // 2. 创建并填充游记基本信息
        TravelPost newPost = new TravelPost();
        newPost.setTitle(dto.getTitle());
        newPost.setContent(dto.getContent());
        newPost.setAuthor(author);

        // 3. 处理并关联地点 (如果提供了SpotId)
        if (dto.getSpotId() != null && !dto.getSpotId().isBlank()) {
            Spot spot = merchantPackageService.findOrCreateSpotByBaiduUid(dto.getSpotId());
            newPost.setSpot(spot);
            log.info("游记已关联到景点: {}", spot.getName());
        }

        // 4. 处理并关联图片集
        for (int i = 0; i < dto.getImageIds().size(); i++) {
            String imgId = dto.getImageIds().get(i);
            MediaFile mediaFile = mediaFileRepository.findById(imgId)
                    .orElseThrow(() -> new BizException("找不到ID为 " + imgId + " 的媒体文件"));

            // 权限校验！确保用户只能用自己上传的图片
            if (!mediaFile.getUploader().getId().equals(currentUserId)) {
                log.warn("权限拒绝: 用户ID '{}' 试图使用不属于自己的图片ID '{}'", currentUserId, imgId);
                throw new BizException(HttpStatus.UNAUTHORIZED.value(), "不能使用不属于自己的图片来创建游记");
            }

            PostImage postImage = new PostImage();
            postImage.setTravelPost(newPost);
            postImage.setMediaFile(mediaFile);
            postImage.setSortOrder(i + 1); // 按前端给的顺序排序

            newPost.getImages().add(postImage);
        }

        // 4.1 处理并关联标签
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(dto.getTagIds());
            if (tags.size() != dto.getTagIds().size()) {
                throw new BizException("部分标签ID无效，请检查后重试");
            }
            newPost.setTags(new HashSet<>(tags));
            log.info("服务层：为新游记关联了 {} 个标签", tags.size());
        }

        // 5. 保存游记
        TravelPost savedPost = postRepository.save(newPost);
        log.info("新游记 '{}' (ID: {}) 已成功创建。", savedPost.getTitle(), savedPost.getId());

        // 6. 通知AI审核文字
        eventPublisher.publishEvent(new ContentSubmissionEvent(
                this,
                savedPost.getId(),
                ContentSubmissionEvent.ModerationItemType.TRAVEL_POST,
                savedPost.getTitle() + "。" + savedPost.getContent()
        ));
        return savedPost;
    }

    /**
     * 更新一个游记的标签
     */
    @Transactional
    public void updatePackageTags(String packageId, List<String> tagIds, String currentDealerId) {
        log.info("服务层：开始为游记ID '{}' 更新标签", packageId);

        // 1. 验证产品是否存在且属于当前经销商
        TravelPost travelPost = travelPostRepository.findById(packageId)
                .orElseThrow(() -> new BizException("找不到对应的用户"));

        if (!travelPost.getAuthor().getId().equals(currentDealerId)) {
            throw new BizException("无权操作不属于自己的游记");
        }

        // 2. 查找新的标签实体列表
        List<Tag> newTags = tagRepository.findAllById(tagIds);
        if (newTags.size() != tagIds.size()) {
            throw new BizException("部分标签ID无效，请检查后重试");
        }

        // 3. 替换标签集合
        travelPost.setTags(new HashSet<>(newTags));

        // 4. 保存更新
        travelPostRepository.save(travelPost);
        log.info("服务层：成功将游记ID '{}' 的标签更新为 {} 个", packageId, newTags.size());
    }

    // 获取当前用户发布的游记列表（分页）
    @Transactional(readOnly = true)
    public PageResponseDto<PostSummaryDto> getMyPosts(String currentUserId, PageRequestDto pageRequestDto) {
        log.info("用户ID '{}' 正在获取自己的游记列表, 分页参数: {}", currentUserId, pageRequestDto);

        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        Page<TravelPost> postPage = postRepository.findByAuthorId(currentUserId, pageable);

        List<PostSummaryDto> dtos = postPage.getContent().stream()
                .map(dtoConverter::convertPostToSummaryDto)
                .collect(Collectors.toList());

        return dtoConverter.convertPageToDto(postPage, dtos);
    }

    // 获取一篇游记的详细信息（权限校验）
    @Transactional(readOnly = true)
    public PostDetailToOwnerDto getMyPostDetails(String postId, String currentUserId) {
        log.info("用户ID '{}' 正在获取游记ID '{}' 的详情...", currentUserId, postId);

        TravelPost post = findPostByIdAndVerifyOwnership(postId, currentUserId);

        return dtoConverter.convertPostToDetailToOwnerDto(post);
    }


    // 删除一篇游记（实现了安全的级联逻辑删除）
    @Transactional
    public void deletePost(String postId, String currentUserId) {
        log.info("用户ID '{}' 正在尝试删除游记ID '{}'...", currentUserId, postId);

        // 1. 查找游记并验证所有权
        TravelPost postToDelete = findPostByIdAndVerifyOwnership(postId, currentUserId);

        // 2. [核心修改] 在删除游记前，先删除所有关联的子记录

        log.info("正在级联删除游记 '{}' 的所有关联数据...", postToDelete.getTitle());

        // a. 删除所有评论
        postCommentRepository.deleteAllByTravelPostId(postId);
        log.info(" -> 已删除游记 '{}' 的所有评论。", postId);

        // b. 删除所有点赞
        likeRepository.deleteAllByItemIdAndItemType(postId, InteractionItemType.POST);
        log.info(" -> 已删除游记 '{}' 的所有点赞记录。", postId);

        // c. 删除所有收藏
        favoriteRepository.deleteAllByItemIdAndItemType(postId, InteractionItemType.POST);
        log.info(" -> 已删除游记 '{}' 的所有收藏记录。", postId);

        // 3. 最后，（逻辑）删除游记本身
        postRepository.delete(postToDelete);

        log.info("游记ID '{}' 及其所有关联数据已成功删除。", postId);
    }

    // 更新一篇游记
    @Transactional
    public TravelPost updatePost(String postId, PostUpdateRequestDto dto, String currentUserId) {
        log.info("用户ID '{}' 正在尝试更新游记ID '{}'...", currentUserId, postId);
        TravelPost postToUpdate = findPostByIdAndVerifyOwnership(postId, currentUserId);

        // 1. 更新基本信息
        postToUpdate.setTitle(dto.getTitle());
        postToUpdate.setContent(dto.getContent());

        // 2. 更新关联地点
        if (dto.getSpotId() != null && !dto.getSpotId().isBlank()) {
            Spot spot = merchantPackageService.findOrCreateSpotByBaiduUid(dto.getSpotId());
            postToUpdate.setSpot(spot);
        } else {
            postToUpdate.setSpot(null); // 如果传入的spotId为空，则解除关联
        }

        // 3. 更新图片列表
        updatePostImages(postToUpdate, dto.getImageIds(), currentUserId);

        // 4. 保存更新
        TravelPost updatedPost = postRepository.save(postToUpdate);
        log.info("游记ID '{}' 已被成功更新。", postId);

        // 5. 通知AI审核文字
        eventPublisher.publishEvent(new ContentSubmissionEvent(
                this,
                updatedPost.getId(),
                ContentSubmissionEvent.ModerationItemType.TRAVEL_POST,
                updatedPost.getTitle() + "。" + updatedPost.getContent()
        ));

        return updatedPost;
    }

    // 私有辅助方法：更新游记的图片列表
    private void updatePostImages(TravelPost post, List<String> imageIds, String currentUserId) {
        post.getImages().clear(); // 清空旧的图片关联

        for (int i = 0; i < imageIds.size(); i++) {
            String imgId = imageIds.get(i);
            MediaFile mediaFile = mediaFileRepository.findById(imgId)
                    .orElseThrow(() -> new BizException("找不到ID为 " + imgId + " 的媒体文件"));

            // 权限校验！确保用户只能用自己上传的图片
            if (!mediaFile.getUploader().getId().equals(currentUserId)) {
                throw new BizException(HttpStatus.UNAUTHORIZED.value(), "不能使用不属于自己的图片");
            }

            PostImage postImage = new PostImage();
            postImage.setTravelPost(post);
            postImage.setMediaFile(mediaFile);
            postImage.setSortOrder(i + 1);
            post.getImages().add(postImage);
        }
    }

    //私有辅助方法：查找游记并验证所有权
    private TravelPost findPostByIdAndVerifyOwnership(String postId, String currentUserId) {
        TravelPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BizException("找不到ID为 " + postId + " 的游记"));

        if (!post.getAuthor().getId().equals(currentUserId)) {
            log.error("权限拒绝: 用户ID '{}' 试图操作不属于自己的游记ID '{}'", currentUserId, postId);
            throw new BizException(HttpStatus.UNAUTHORIZED.value(), "无权操作此游记");
        }
        return post;
    }
}
