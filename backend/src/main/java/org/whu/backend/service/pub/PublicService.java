package org.whu.backend.service.pub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.accounts.ShareDto;
import org.whu.backend.dto.accounts.UserProfileDto;
import org.whu.backend.dto.post.PostDetailDto;
import org.whu.backend.dto.post.PostSearchRequestDto;
import org.whu.backend.dto.post.PostSummaryDto;
import org.whu.backend.dto.travelpack.DepartureSummaryDto;
import org.whu.backend.dto.travelpack.PackageDetailDto;
import org.whu.backend.dto.travelpack.PackageSearchRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.TravelDeparture;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.accounts.Account;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.repository.authRepo.AuthRepository;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.repository.post.TravelPostRepository;
import org.whu.backend.repository.travelRepo.TravelDepartureRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.ViewCountService;
import org.whu.backend.service.specification.SearchSpecification;
import org.whu.backend.util.AccountUtil;
import org.whu.backend.util.AliyunOssUtil;
import org.whu.backend.util.JpaUtil;

import java.util.*;
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
    @Autowired
    private ViewCountService viewCountService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TravelDepartureRepository travelDepartureRepository;
    @Autowired
    private AccountUtil accountUtil;
    @Autowired
    private AuthRepository authRepository;

    // 获取已发布的旅行团列表（分页）
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

        return dtoConverter.convertPageToDto(packagePage, summaryDtos);
    }

    // 获取单个旅行团的详情
    public PackageDetailDto getPackageDetails(String id, String ipAddress) {
        log.info("IP地址 '{}' 正在获取旅行团ID '{}' 的详情...", ipAddress, id);

        // 1. 调用防刷服务来尝试增加浏览量
        // 这个操作是异步的或者非常快，不会阻塞主流程
        try {
            viewCountService.incrementViewCountIfAbsent(id, ipAddress, ViewCountService.EntityType.TRAVEL_PACKAGE);
        } catch (Exception e) {
            // 即使浏览量增加失败（比如Redis挂了），也绝不能影响用户看详情
            log.error("增加浏览量失败，但不影响主流程。Package ID: {}, IP: {}", id, ipAddress, e);
        }

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

    /**
     * 【新增】获取指定产品的可报名团期列表（分页）
     */
    public PageResponseDto<DepartureSummaryDto> getAvailableDeparturesForPackage(String packageId, PageRequestDto pageRequestDto) {
        log.info("服务层：开始查询产品ID '{}' 的可报名团期列表...", packageId);

        // 1. 验证产品是否存在且已发布，防止查询到草稿或已下架产品的团期
        if (!travelPackageRepository.existsByIdAndStatus(packageId, TravelPackage.PackageStatus.PUBLISHED)) {
            throw new BizException("该旅游产品不存在或未发布");
        }

        // 2. 创建分页请求，默认按出发日期升序排序
        Sort sort = Sort.by(Sort.Direction.ASC, "departureDate");
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        // 3. 查询状态为OPEN（可报名）的团期
        Page<TravelDeparture> departurePage = travelDepartureRepository.findByTravelPackageIdAndStatus(
                packageId,
                TravelDeparture.DepartureStatus.OPEN,
                pageable
        );
        log.info("服务层：为产品ID '{}' 查询到 {} 个可报名团期。", packageId, departurePage.getTotalElements());

        // 4. 将查询结果转换为DTO
        List<DepartureSummaryDto> dtos = departurePage.getContent().stream()
                .map(dtoConverter::convertDepartureToSummaryDto)
                .collect(Collectors.toList());

        return dtoConverter.convertPageToDto(departurePage, dtos);
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

        return dtoConverter.convertPageToDto(postPage, dtos);
    }

    // 实现复杂的、多条件的搜索逻辑
    public PageResponseDto<PackageSummaryDto> searchPackages(PackageSearchRequestDto searchDto) {
        log.info("开始复杂条件搜索旅行团, 搜索条件: {}", searchDto);

        // 1. 使用 Specification 构建动态查询条件
        Specification<TravelPackage> spec = SearchSpecification.from(searchDto);

        // 2. 创建分页和排序对象
        Sort sort = Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy());
        Pageable pageable = PageRequest.of(searchDto.getPage() - 1, searchDto.getSize(), sort);

        // 3. 执行查询！
        Page<TravelPackage> packagePage = travelPackageRepository.findAll(spec, pageable);

        log.info("查询到 {} 条记录，总共 {} 页。", packagePage.getNumberOfElements(), packagePage.getTotalPages());

        // 4. 转换并返回结果 (这部分逻辑不变)
        List<PackageSummaryDto> summaryDtos = packagePage.getContent().stream()
                .map(dtoConverter::convertPackageToSummaryDto)
                .toList();

        return dtoConverter.convertPageToDto(packagePage, summaryDtos);
    }

    /**
     * 实现复杂的、多条件的游记搜索逻辑
     */
    public PageResponseDto<PostSummaryDto> searchPosts(PostSearchRequestDto searchDto) {
        log.info("开始复杂条件搜索游记, 搜索条件: {}", searchDto);

        Specification<TravelPost> spec = SearchSpecification.from(searchDto);

        Pageable pageable = PageRequest.of(searchDto.getPage() - 1, searchDto.getSize(),
                Sort.by(Sort.Direction.fromString(searchDto.getSortDirection()), searchDto.getSortBy()));

        Page<TravelPost> postPage = travelPostRepository.findAll(spec, pageable);

        List<PostSummaryDto> postSummaryDtos = postPage.getContent().stream().
                map(dtoConverter::convertPostToSummaryDto)
                .toList();

        return dtoConverter.convertPageToDto(postPage, postSummaryDtos);
    }

    // 获取单篇已发布的游记详情
    public PostDetailDto getPostDetails(String postId, String ipAddress) {
        log.info("正在获取公共游记详情, ID: {}", postId);

        TravelPost post = travelPostRepository.findById(postId)
                .orElseThrow(() -> new BizException("找不到ID为 " + postId + " 的游记"));

        // 调用防刷服务来尝试增加浏览量
        try {
            viewCountService.incrementViewCountIfAbsent(postId, ipAddress, ViewCountService.EntityType.POST);
        } catch (Exception e) {
            log.error("增加浏览量失败，但不影响主流程。Package ID: {}, IP: {}", postId, ipAddress, e);
        }

        // TODO: 可以在这里增加一个状态判断，比如只返回状态为“已发布”的游记，还有只查询公共权限的游记

        return dtoConverter.convertPostToDetailDto(post);
    }

    /**
     * 获取指定用户的公开主页信息
     */
    public UserProfileDto getUserProfile(String userId) {
        log.info("正在查询用户ID '{}' 的公开主页信息...", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("找不到ID为 " + userId + " 的用户"));

        return dtoConverter.ConvertUserToUserProfileDto(user);
    }

    /**
     * 获取指定用户发布的游记列表（分页）
     */
    public PageResponseDto<PostSummaryDto> getUserPosts(String userId, PageRequestDto pageRequestDto) {
        log.info("正在查询用户ID '{}' 发布的游记列表, 分页参数: {}", userId, pageRequestDto);

        // 验证用户是否存在
        if (!userRepository.existsById(userId)) {
            throw new BizException("找不到ID为 " + userId + " 的用户");
        }

        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        Page<TravelPost> postPage = travelPostRepository.findByAuthorId(userId, pageable);

        return dtoConverter.convertPageToDto(postPage,
                postPage.getContent().stream().map(dtoConverter::convertPostToSummaryDto).toList());
    }

    public ShareDto getUserInfos(String userId) {
        Account account = authRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        ShareDto dto = new ShareDto();
        dto.setId(account.getId());
        dto.setRole(account.getRole());
        dto.setUsername(account.getUsername());
        dto.setAvatarUrl(AliyunOssUtil.generatePresignedGetUrl(account.getAvatarUrl(),36000));
        dto.setActive(account.isActive());

        return dto;
    }

    private boolean isUUID(String input) {
        try {
            UUID.fromString(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    private ShareDto toShareDto(Account user) {
        ShareDto dto = new ShareDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatarUrl(AliyunOssUtil.generatePresignedGetUrl(user.getAvatarUrl(),36000));
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        return dto;
    }

    public List<ShareDto> searchUsers(String keyword) {
        Set<String> seenIds = new HashSet<>();
        List<ShareDto> results = new ArrayList<>();
        String currentUserId=AccountUtil.getCurrentAccountId();

        if (isUUID(keyword)) {
            authRepository.findById(keyword).ifPresent(user -> {
                if (!user.getId().equals(currentUserId) && seenIds.add(user.getId())) {
                    results.add(toShareDto(user));
                }
            });
        }

        authRepository.findByPhone(keyword).ifPresent(user -> {
            if (!user.getId().equals(currentUserId) && seenIds.add(user.getId())) {
                results.add(toShareDto(user));
            }
        });

        authRepository.findByEmail(keyword).ifPresent(user -> {
            if (!user.getId().equals(currentUserId) && seenIds.add(user.getId())) {
                results.add(toShareDto(user));
            }
        });

        List<Account> fuzzyUsers = authRepository.findByUsernameContaining(keyword);
        for (Account user : fuzzyUsers) {
            if (!user.getId().equals(currentUserId) && seenIds.add(user.getId())) {
                results.add(toShareDto(user));
            }
        }

        return results;
    }
}
