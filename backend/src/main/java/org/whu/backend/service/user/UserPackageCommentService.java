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
import org.whu.backend.dto.packagecomment.PackageCommentCreateDto;
import org.whu.backend.dto.packagecomment.PackageCommentDto;
import org.whu.backend.entity.Order;
import org.whu.backend.entity.PackageComment;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.repository.travelRepo.OrderRepository;
import org.whu.backend.repository.travelRepo.PackageCommentRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.springframework.util.StringUtils;
import org.whu.backend.service.DtoConverter;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserPackageCommentService {

    @Autowired
    private PackageCommentRepository commentRepository;
    @Autowired
    private TravelPackageRepository packageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PackageCommentRepository packageCommentRepository;
    @Autowired
    private DtoConverter dtoConverter;

    @Transactional
    public PackageComment createComment(String packageId, PackageCommentCreateDto dto, String currentUserId) {
        log.info("用户ID '{}' 正在为旅行团ID '{}' 发布新评价...", currentUserId, packageId);

        // 1. 验证用户、旅行团是否存在
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BizException("找不到用户 " + currentUserId));
        TravelPackage travelPackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new BizException("找不到旅行团 " + packageId));

        // 2.【重要】业务规则校验：用户必须完成过该旅行团才能评价
        boolean hasPurchased = orderRepository.existsByUserAndTravelPackageAndStatus(author, travelPackage, Order.OrderStatus.COMPLETED);
        if (!hasPurchased) {
            throw new BizException("尚未购买该旅行团，无法评价");
        }

        // 3. 创建新评价
        PackageComment newComment = new PackageComment();
        newComment.setRating(dto.getRating());
        newComment.setContent(dto.getContent());
        newComment.setAuthor(author);
        newComment.setTravelPackage(travelPackage);

        // 4. 如果是回复，处理父子关系
        if (dto.getParentId() != null && !dto.getParentId().isBlank()) {
            PackageComment parentComment = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new BizException("找不到要回复的评价 " + dto.getParentId()));

            // 安全校验：确保父评价属于同一个旅行团
            if (!parentComment.getTravelPackage().getId().equals(packageId)) {
                throw new BizException("无效操作：不能跨越不同的旅行团进行回复。");
            }

            if (!(parentComment.getParent() == null || parentComment.getParent().getId() == null || parentComment.getParent().getId().isEmpty())) {
                throw new BizException("不允许回复楼中楼");
            }
            newComment.setParent(parentComment);
        }

        return commentRepository.save(newComment);
    }

    // 获取指定旅行团的评价列表（分页）
    public PageResponseDto<PackageCommentDto> getPackageComments(String packageId, PageRequestDto pageRequestDto) {
        log.info("正在获取旅行团ID '{}' 的评价列表...", packageId);

        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(Sort.Direction.DESC, "createdTime")); // 评价通常按时间倒序

        Page<PackageComment> commentPage = packageCommentRepository.findByTravelPackageIdAndParentIsNull(packageId, pageable);

        List<PackageCommentDto> dtos = commentPage.getContent().stream()
                .map(comment -> {
                    // 为每条一级评价查找它的前3条二级回复作为预览
                    List<PackageCommentDto> repliesPreview = packageCommentRepository.findTop3ByParentIdOrderByCreatedTimeAsc(comment.getId())
                            .stream()
                            .map(dtoConverter::convertPackageCommentToSimpleDto) // 回复用简单转换
                            .collect(Collectors.toList());

                    long totalReplies = packageCommentRepository.countByParentId(comment.getId());

                    return dtoConverter.convertPackageCommentToDto(comment, repliesPreview, totalReplies);
                })
                .collect(Collectors.toList());

        return PageResponseDto.<PackageCommentDto>builder()
                .content(dtos)
                .pageNumber(commentPage.getNumber() + 1)
                .pageSize(commentPage.getSize())
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .first(commentPage.isFirst())
                .last(commentPage.isLast())
                .numberOfElements(commentPage.getNumberOfElements())
                .build();
    }

//    /**
//     * [已修改] 获取单条旅行团评价的详细信息
//     * 现在它会接收一个用于其回复列表的分页参数
//     */
//    public PackageCommentDto getPackageCommentDetails(String commentId, PageRequestDto repliesPageRequest) {
//        log.info("正在获取旅行团评价ID '{}' 的详情, 回复分页参数: {}", commentId, repliesPageRequest);
//
//        // 1. 查找主评价是否存在
//        PackageComment mainComment = packageCommentRepository.findById(commentId)
//                .orElseThrow(() -> new BizException("找不到ID为 " + commentId + " 的评价"));
//
//        // 2. [修改] 分页查询该主评价下的所有直接回复
//        Pageable repliesPageable = PageRequest.of(
//                repliesPageRequest.getPage() - 1,
//                repliesPageRequest.getSize(),
//                Sort.by(Sort.Direction.ASC, "createdTime") // 回复按时间正序
//        );
//        Page<PackageComment> repliesPage = packageCommentRepository.findByParentId(commentId, repliesPageable);
//
//        // 3. 将回复的分页结果(Page<Entity>)转换为我们自定义的分页DTO(PageResponseDto<DTO>)
//        PageResponseDto<PackageCommentDto> repliesPageDto = dtoConverter.convertPageToDto(
//                repliesPage,
//                dtoConverter::convertPackageCommentToSimpleDto
//        );
//
//        // 4. 将主评价和它的回复分页数据组装成最终的DTO
//        return dtoConverter.convertPackageCommentToDtoWithReplies(mainComment, repliesPageDto);
//    }
}