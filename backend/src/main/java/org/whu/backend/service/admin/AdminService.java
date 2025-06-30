package org.whu.backend.service.admin;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.accounts.MerchantSummaryDto;
import org.whu.backend.dto.admin.BanRequestDto;
import org.whu.backend.dto.admin.RejectionRequestDto;
import org.whu.backend.dto.admin.UserManagementDto;
import org.whu.backend.dto.spot.SpotCreateRequestDto;
import org.whu.backend.dto.spot.SpotSummaryDto;
import org.whu.backend.dto.spot.SpotUpdateRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.Spot;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.accounts.*;
import org.whu.backend.entity.travelpost.Notification;
import org.whu.backend.repository.authRepo.MerchantRepository;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.repository.travelRepo.SpotRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.NotificationService;
import org.whu.backend.util.AccountUtil;
import org.whu.backend.util.JpaUtil;
import org.whu.backend.repository.authRepo.AuthRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.whu.backend.entity.TravelPackage.PackageStatus.*;
import static org.whu.backend.entity.accounts.Merchant.status.APPROVED;
import static org.whu.backend.entity.accounts.Merchant.status.PENDING;

@Slf4j
@Service
public class AdminService {

    @Autowired
    private TravelPackageRepository travelPackageRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private SpotRepository spotRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthRepository accountRepository;
    @Autowired
    private DtoConverter dtoConverter;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AccountUtil securityUtil;
//    @Autowired
//    private JpaUtil jpaUtil;

    public PageResponseDto<PackageSummaryDto> getPendingPackages(PageRequestDto pageRequestDto) {
        // 1️获取分页参数和排序参数
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(direction, pageRequestDto.getSortBy()));

        // 2查询数据库：状态为 PENDING_APPROVAL
        Page<TravelPackage> page = travelPackageRepository.findByStatus(PENDING_APPROVAL, pageable);

        // 3️转换为 DTO
        List<PackageSummaryDto> content = page.getContent().stream()
                .map(dtoConverter::convertPackageToSummaryDto)
                .toList();

        // 4️构建 PageResponseDto
        return PageResponseDto.<PackageSummaryDto>builder()
                .content(content)
                .pageNumber(pageRequestDto.getPage())
                .pageSize(pageRequestDto.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }

    public boolean approvePackage(String packageId) {
        // 1️根据 id 获取旅行团
        TravelPackage travelPackage = JpaUtil.getOrThrow(travelPackageRepository, packageId, "旅行团不存在");
        Merchant merchant = travelPackage.getDealer();
        Account admin = securityUtil.getCurrentAccount();

        // 2️检查状态是否为 PENDING_APPROVAL
        if (!PENDING_APPROVAL.equals(travelPackage.getStatus())) {
            throw new BizException("当前状态不可审批");
        }

        // 3️修改状态为 PUBLISHED
        travelPackage.setStatus(PUBLISHED);
        travelPackageRepository.save(travelPackage);

        // 发送审核通过通知
        String description = String.format("旅行团 %s 已成功通过审核", travelPackage.getTitle());
        notificationService.createAndSendNotification(
                merchant,
                Notification.NotificationType.PACKAGE_APPROVED,
                description,
                null,
                admin,
                travelPackage.getId());

        return true;
    }

    public boolean rejectPackage(String packageId, RejectionRequestDto rejectionDto) {
        // 1️获取旅行团
        TravelPackage travelPackage = JpaUtil.getOrThrow(travelPackageRepository, packageId, "旅行团不存在");
        Merchant merchant = travelPackage.getDealer();
        Account admin = securityUtil.getCurrentAccount();

        // 2️检查状态是否为 PENDING_APPROVAL
        if (!PENDING_APPROVAL.equals(travelPackage.getStatus())) {
            throw new BizException("当前状态不可驳回");
        }

        // 3️修改状态为 REJECTED，并记录原因
        travelPackage.setStatus(REJECTED);
        travelPackage.setRejectionReason(rejectionDto.getReason());
        travelPackageRepository.save(travelPackage);

        // 发送审核被拒绝通知
        String description = String.format("旅行团 %s 未通过审核", travelPackage.getTitle());
        notificationService.createAndSendNotification(
                merchant,
                Notification.NotificationType.PACKAGE_REJECTED,
                description,
                rejectionDto.getReason(),
                admin,
                travelPackage.getId());
        return true;
    }


    public PageResponseDto<MerchantSummaryDto> getPendingMerchants(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1,
                pageRequestDto.getSize(),
                Sort.by(direction, pageRequestDto.getSortBy()));
        Page<Merchant> page = merchantRepository.findByApproval(PENDING, pageable);

        List<MerchantSummaryDto> content = page.getContent().stream()
                .map(mc -> {
                    MerchantSummaryDto dto = new MerchantSummaryDto();
                    dto.setId(mc.getId());
                    dto.setName(mc.getUsername());
                    dto.setBusinessLicenseUrl(mc.getBusinessLicenseUrl());
                    dto.setBusinessPermitUrl(mc.getBusinessPermitUrl());
                    dto.setIdCardUrl1(mc.getIdCardUrl1());
                    dto.setIdCardUrl2(mc.getIdCardUrl2());
                    return dto;
                })
                .toList();

        return PageResponseDto.<MerchantSummaryDto>builder()
                .content(content)
                .pageNumber(pageRequestDto.getPage())
                .pageSize(pageRequestDto.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }


    public boolean approveMerchants(@PathVariable String merchantId) {
        Merchant merchant = JpaUtil.getOrThrow(merchantRepository, merchantId, "商家不存在");
        if (!PENDING.equals(merchant.getApproval())) {
            throw new BizException("当前状态不可审批");
        }
        merchant.setApproval(APPROVED);
        merchantRepository.save(merchant);

//TODO: 记录审批历史
//        MerchantApprovalRecord record = new MerchantApprovalRecord();
//        record.setMerchantId(merchantId);
//        record.setStatus("APPROVED");
//        record.setReason("审批通过"); // 可选字段
//        record.setCreatedAt(LocalDateTime.now());
//        record.setOperatorId("当前操作用户ID"); // 可获取登录态
//        merchantApprovalRecordRepository.save(record);

        return true;
    }


    public boolean rejectMerchants(@PathVariable String merchantId, @RequestBody RejectionRequestDto rejectionDto) {
        Merchant merchant = JpaUtil.getOrThrow(merchantRepository, merchantId, "商家不存在");
        if (!PENDING.equals(merchant.getApproval())) {
            throw new BizException("当前状态不可驳回");
        }

        merchant.setApproval(Merchant.status.REJECTED);
        merchant.setRejectionReason(rejectionDto.getReason());
        merchantRepository.save(merchant);

//TODO: 记录驳回原因
//        MerchantApprovalRecord record = new MerchantApprovalRecord();
//        record.setMerchantId(merchantId);
//        record.setStatus("REJECTED");
//        record.setReason(rejectionDto.getReason());
//        record.setCreatedAt(LocalDateTime.now());
//        record.setOperatorId("当前操作用户ID");
//        merchantApprovalRecordRepository.save(record);

        return true;
    }

    // TODO: 未来可以增加经销商注册审核的相关接口

    public SpotSummaryDto createSpot(SpotCreateRequestDto spotCreateRequestDto) {
        JpaUtil.notNull(spotCreateRequestDto, "请求参数不能为空");

        // 2️检查是否已存在相同 mapProviderUid 的景点
        Optional<Spot> existing = spotRepository.findByMapProviderUid(spotCreateRequestDto.getMapProviderUid());
        JpaUtil.isTrue(existing.isEmpty(), "该景点已存在");

        Spot spot = new Spot();
        spot.setMapProviderUid(spotCreateRequestDto.getMapProviderUid());
        spot.setName(spotCreateRequestDto.getName());
        spot.setAddress(spotCreateRequestDto.getAddress());
        spot.setCity(spotCreateRequestDto.getCity());
        spot.setLongitude(spotCreateRequestDto.getLongitude());
        spot.setLatitude(spotCreateRequestDto.getLatitude());

        spot = spotRepository.save(spot);

        return new SpotSummaryDto(spot.getId(),
                spot.getName(),
                spot.getAddress(),
                spot.getCity(),
                spot.getLongitude(),
                spot.getLatitude());

    }

    public SpotSummaryDto updateSpot(String id, SpotUpdateRequestDto spotUpdateRequestDto) {
        JpaUtil.notNull(spotUpdateRequestDto, "更新参数不能为空");
        Spot spot = JpaUtil.getOrThrow(spotRepository, id, "景点不存在");
        // 3️更新字段
        spot.setName(spotUpdateRequestDto.getName());
        // 4️保存数据库
        spot = spotRepository.save(spot);
        // 返回 DTO
        return new SpotSummaryDto(spot.getId(),
                spot.getName(),
                spot.getAddress(),
                spot.getCity(),
                spot.getLongitude(),
                spot.getLatitude());
    }


    public boolean deleteSpot(@PathVariable String id) {
        JpaUtil.notNull(id, "景点ID不能为空");
        Spot spot = JpaUtil.getOrThrow(spotRepository, id, "景点不存在");
        spotRepository.delete(spot);
        return true;
    }


    public PageResponseDto<SpotSummaryDto> getSpots(@Valid @ParameterObject PageRequestDto pageRequestDto) {
        // 1️检查参数
        JpaUtil.notNull(pageRequestDto, "分页参数不能为空");
        // 2获取分页和排序参数
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1,
                pageRequestDto.getSize(),
                Sort.by(direction, pageRequestDto.getSortBy()));
        // 3️获取分页结果
        Page<Spot> page = spotRepository.findAll(pageable);

        // 4️转换为 DTO 列表
        List<SpotSummaryDto> content = page.getContent().stream()
                .map(spot -> {
                    return new SpotSummaryDto(spot.getId(),
                            spot.getName(),
                            spot.getAddress(),
                            spot.getCity(),
                            spot.getLongitude(),
                            spot.getLatitude());
                })
                .toList();
        // 5️构建 PageResponseDto
        return PageResponseDto.<SpotSummaryDto>builder()
                .content(content)
                .pageNumber(pageRequestDto.getPage())
                .pageSize(pageRequestDto.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }

    public PageResponseDto<UserManagementDto> getAllUsers(PageRequestDto pageRequestDto, String role, String id, String name) {
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(),
                Sort.by(direction, pageRequestDto.getSortBy()));

        Page<Account> page;

        // 1️精确查 ID：优先级最高
        if (id != null && !id.isBlank()) {
            Optional<Account> accountOpt = accountRepository.findById(id);
            if (accountOpt.isEmpty()) {
                throw new BizException("用户不存在");
            }
            Account account = accountOpt.get();
            UserManagementDto dto = mapToDto(account);
            return PageResponseDto.<UserManagementDto>builder()
                    .content(List.of(dto))
                    .pageNumber(1)
                    .pageSize(1)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .numberOfElements(1)
                    .build();
        }

        // 2️模糊查用户名 + 角色
        if (name != null && !name.isBlank()) {
            if (role != null && !role.isBlank()) {
                Role enumRole = Role.valueOf(role);
                page = accountRepository.findByUsernameContainingAndRole(name, enumRole, pageable);
            } else {
                page = accountRepository.findByUsernameContaining(name, pageable);
            }
        }
        // 3️只查角色
        else if (role != null && !role.isBlank()) {
            Role enumRole = Role.valueOf(role);
            page = accountRepository.findByRole(enumRole, pageable);
        }
        // 4️查所有
        else {
            page = accountRepository.findAll(pageable);
        }

        List<UserManagementDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PageResponseDto.<UserManagementDto>builder()
                .content(content)
                .pageNumber(pageRequestDto.getPage())
                .pageSize(pageRequestDto.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }

    public boolean banAccount(String accountId, BanRequestDto banRequestDto) {
        JpaUtil.notNull(accountId, "账户ID不能为空");
        JpaUtil.notNull(banRequestDto, "封禁请求不能为空");
        Account account = JpaUtil.getOrThrow(userRepository, accountId, "账户不存在");
        // 2️判断是否已被封禁
        if (account.getBanDurationDays() != 0)
            throw new BizException("账户已被封禁");
        // 3️设置封禁状态和时间
        //account.setEnabled(false); // 禁用账户
        account.setBanDurationDays(banRequestDto.getDurationInDays()); // 封禁时长
        account.setBanStartTime(LocalDateTime.now()); // 封禁起始时间
        account.setBanReason(banRequestDto.getReason()); // 封禁原因
        // 4️保存账户
        accountRepository.save(account);
        return true;
    }

    public boolean unbanAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BizException("账户不存在"));

        if (account.getBanDurationDays() == -1) {
        } else if (account.getBanDurationDays() == 0) {
            throw new BizException("账户当前未被封禁");
        } else if (account.getBanDurationDays() > 0 && account.getBanStartTime() != null) {
            LocalDateTime banEndTime = account.getBanStartTime().plusDays(account.getBanDurationDays());
            if (LocalDateTime.now().isAfter(banEndTime)) {
                throw new BizException("账户封禁期已过，无需解封");
            }
        }
        // 解除封禁
        //account.setEnabled(true);
        account.setBanDurationDays(0);
        account.setBanStartTime(null);
        account.setBanReason(null);

        accountRepository.save(account);

        return true;
    }

    private UserManagementDto mapToDto(Account account) {
        UserManagementDto dto = new UserManagementDto();
        dto.setId(account.getId());
        dto.setEmail(account.getEmail());
        dto.setPhone(account.getPhone());
        dto.setUsername(account.getUsername());
        dto.setRole(account.getRole());
        dto.setBanDurationDays(account.getBanDurationDays());
        dto.setBanStartTime(account.getBanStartTime());
        dto.setCreatedAt(account.getCreatedAt());
        return dto;
    }
}
