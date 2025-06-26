package org.whu.backend.service;

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
import org.whu.backend.dto.post.PostDetailDto;
import org.whu.backend.dto.post.PostSummaryDto;
import org.whu.backend.dto.travelpack.PackageDetailDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.repository.TravelPostRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PublicService {
    @Autowired
    private TravelPackageRepository travelPackageRepository;
    @Autowired
    private DtoConverter dtoConverter;
    @Autowired
    private TravelPostRepository travelPostRepository;
//    @Autowired
//    private TravelPostRepository travelPostRepository;
//    @Autowired
//    private UserPostService userPostService;

    /**
     * 获取已发布的旅行团列表（分页）
     */
    public PageResponseDto<PackageSummaryDto> getPublishedPackages(PageRequestDto pageRequestDto) {
        log.info("开始查询已发布的旅行团列表，分页参数: {}", pageRequestDto);

        // 1. 从DTO创建JPA需要的分页和排序对象
        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        // 2. 调用Repository中定义好的方法，查询的是PUBLISHED的旅游团
        Page<TravelPackage> packagePage = travelPackageRepository.findByStatus(TravelPackage.PackageStatus.PUBLISHED, pageable);
        log.info("查询到 {} 条记录，总共 {} 页。", packagePage.getNumberOfElements(), packagePage.getTotalPages());

        // 3. 将查询到的 Page<Entity> 转换为 List<DTO>
        List<PackageSummaryDto> summaryDtos = packagePage.getContent().stream()
                .map(dtoConverter::convertPackageToSummaryDto)
                .collect(Collectors.toList());

        // 4. 使用Builder模式构建分页响应对象
        return PageResponseDto.<PackageSummaryDto>builder()
                .content(summaryDtos)
                .pageNumber(packagePage.getNumber() + 1)
                .pageSize(packagePage.getSize())
                .totalElements(packagePage.getTotalElements())
                .totalPages(packagePage.getTotalPages())
                .first(packagePage.isFirst())
                .last(packagePage.isLast())
                .numberOfElements(packagePage.getNumberOfElements())
                .build();
    }

    /**
     * 获取单个旅行团的详情
     */
    public PackageDetailDto getPackageDetails(String id) {
        log.info("开始查询ID为 '{}' 的旅行团详情。", id);

        // 1. 调用Repository中定义好的方法
        TravelPackage travelPackage = travelPackageRepository.findByIdAndStatus(id, TravelPackage.PackageStatus.PUBLISHED)
                .orElseThrow(() -> {
                    log.warn("查询失败：找不到ID为 '{}' 的旅行团，或该旅行团尚未发布。", id);
                    return new BizException("找不到ID为 " + id + " 的旅行团，或该旅行团尚未发布。");
                });
        log.info("成功查询到旅行团 '{}' 的详情。", travelPackage.getTitle());
        // 2. 将Entity转换为详细的DTO
        return dtoConverter.convertPackageToDetailDto(travelPackage);
    }


    // 获取已发布的游记列表（分页）
    public PageResponseDto<PostSummaryDto> getPublishedPosts(PageRequestDto pageRequestDto) {
        log.info("正在获取公共游记列表, 分页参数: {}", pageRequestDto);

        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        // JpaRepository自带的findAll(pageable)即可
        Page<TravelPost> postPage = travelPostRepository.findAll(pageable);

        List<PostSummaryDto> dtos = postPage.getContent().stream()
                .map(dtoConverter::convertPostToSummaryDto)
                .collect(Collectors.toList());

        return PageResponseDto.<PostSummaryDto>builder()
                .content(dtos)
                .pageNumber(postPage.getNumber() + 1)
                .pageSize(postPage.getSize())
                .totalElements(postPage.getTotalElements())
                .totalPages(postPage.getTotalPages())
                .first(postPage.isFirst())
                .last(postPage.isLast())
                .numberOfElements(postPage.getNumberOfElements())
                .build();
    }

    // [新增] 获取单篇已发布的游记详情
    public PostDetailDto getPostDetails(String postId) {
        log.info("正在获取公共游记详情, ID: {}", postId);

        TravelPost post = travelPostRepository.findById(postId)
                .orElseThrow(() -> new BizException("找不到ID为 " + postId + " 的游记"));

        // TODO: 可以在这里增加一个状态判断，比如只返回状态为“已发布”的游记，还有只查询公共权限的游记

        return dtoConverter.convertPostToDetailDto(post);
    }
}
