package org.whu.backend.service.admin;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.accounts.MerchantSummaryDto;
import org.whu.backend.dto.accounts.MerchantrankDto;
import org.whu.backend.dto.admin.BanRequestDto;
import org.whu.backend.dto.admin.RejectionRequestDto;
import org.whu.backend.dto.admin.UserManagementDto;
import org.whu.backend.dto.spot.SpotCreateRequestDto;
import org.whu.backend.dto.spot.SpotSummaryDto;
import org.whu.backend.dto.spot.SpotUpdateRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.Spot;
import org.whu.backend.entity.travelpac.PackageComment;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.entity.accounts.*;
import org.whu.backend.entity.Notification;
import org.whu.backend.repository.authRepo.MerchantRepository;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.repository.projection.TimeCount;
import org.whu.backend.repository.travelRepo.*;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.NotificationService;
import org.whu.backend.util.AccountUtil;
import org.whu.backend.util.AliyunOssUtil;
import org.whu.backend.util.ChartDataUtil;
import org.whu.backend.util.JpaUtil;
import org.whu.backend.repository.authRepo.AuthRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;

import static org.whu.backend.entity.travelpac.TravelPackage.PackageStatus.*;
import static org.whu.backend.entity.accounts.Merchant.status.APPROVED;
import static org.whu.backend.entity.accounts.Merchant.status.PENDING;
import static org.whu.backend.service.DtoConverter.IMAGE_PROCESS;

@Slf4j
@Service
public class AdminService {

    @Autowired
    private TravelPackageRepository travelPackageRepository;
    @Autowired
    private TravelDepartureRepository travelDepartureRepository;
    @Autowired
    private TravelOrderRepository travelOrderRepository;
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
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PackageCommentRepository packageCommentRepository;
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
        String description = String.format("旅行团 [%s] 已成功通过审核", travelPackage.getTitle());
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
        String description = String.format("旅行团 [%s] 未通过审核", travelPackage.getTitle());
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
                .map(spot -> new SpotSummaryDto(spot.getId(),
                        spot.getName(),
                        spot.getAddress(),
                        spot.getCity(),
                        spot.getLongitude(),
                        spot.getLatitude()))
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


    //--------data
    public Map<String, Object> getUserGrowthData(String period, Role role,
                                                 LocalDate startDate, LocalDate endDate) {
        // 1. 统一计算时间范围，支持默认值和校验
        Pair<LocalDate, LocalDate> range = ChartDataUtil.resolveRange(period, startDate, endDate);
        startDate = range.getLeft();
        endDate = range.getRight();

        // 2. 统一转换为 LocalDateTime 左闭右开区间：[startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
        Pair<LocalDateTime, LocalDateTime> dateTimeRange = ChartDataUtil.toDateTimeRange(startDate, endDate);
        LocalDateTime start = dateTimeRange.getLeft();
        LocalDateTime end = dateTimeRange.getRight();

        List<Object[]> rawData;
        switch (period.toLowerCase()) {
            case "day" -> rawData = accountRepository.countByDay(start, end, role);
            case "week" -> rawData = accountRepository.countByWeek(start, end, role);
            case "month" -> rawData = accountRepository.countByMonth(start, end, role);
            default -> throw new IllegalArgumentException("不支持的 period 类型: " + period);
        }

        // 3. 调用工具类构造图表数据，附加字段名 role，值为角色名或 "ALL"
        String roleLabel = role != null ? role.name() : "ALL";
        return ChartDataUtil.buildChartData(period, startDate, endDate, rawData, "role", roleLabel);
    }

    /**
     * 获取基于时间粒度的出发团期增长统计数据
     *
     * @param period    时间粒度，支持 "day"、"week"、"month"
     * @param startDate 起始日期（包含）
     * @param endDate   结束日期（包含）
     * @return 统计结果，包含 xAxis、yAxis、total 等字段
     */
    public Map<String, Object> getTravelDepartureGrowth(String period, LocalDate startDate, LocalDate endDate) {
        // 1. 统一计算时间范围，支持默认值和校验
        Pair<LocalDate, LocalDate> range = ChartDataUtil.resolveRange(period, startDate, endDate);
        startDate = range.getLeft();
        endDate = range.getRight();

        // 2. 统一转换为 LocalDateTime 左闭右开区间：[startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
        Pair<LocalDateTime, LocalDateTime> dateTimeRange = ChartDataUtil.toDateTimeRange(startDate, endDate);
        LocalDateTime start = dateTimeRange.getLeft();
        LocalDateTime end = dateTimeRange.getRight();

        List<Object[]> rawData;
        switch (period.toLowerCase()) {
            case "day" -> rawData = travelDepartureRepository.countByDay(start, end);
            case "week" -> rawData = travelDepartureRepository.countByWeek(start, end);
            case "month" -> rawData = travelDepartureRepository.countByMonth(start, end);
            default -> throw new IllegalArgumentException("不支持的 period 类型: " + period);
        }

        // 3. 调用工具类构造图表数据，附加字段名 type，值固定为 "travelDeparture"
        return ChartDataUtil.buildChartData(period, startDate, endDate, rawData, "type", "travelDeparture");
    }

    public Map<String, Object> getOrderStats(String period, LocalDate startDate, LocalDate endDate, String merchantId) {

        // 1. 统一计算时间范围，支持默认值和校验
        Pair<LocalDate, LocalDate> range = ChartDataUtil.resolveRange(period, startDate, endDate);
        startDate = range.getLeft();
        endDate = range.getRight();

        // 2. 统一转换为 LocalDateTime 左闭右开区间：[startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
        Pair<LocalDateTime, LocalDateTime> dateTimeRange = ChartDataUtil.toDateTimeRange(startDate, endDate);
        LocalDateTime start = dateTimeRange.getLeft();
        LocalDateTime end = dateTimeRange.getRight();

        // 3. 查询数据
        List<Object[]> rawData = switch (period.toLowerCase()) {
            case "day" -> travelOrderRepository.countOrderStatsByDay(start, end, merchantId);
            case "week" -> travelOrderRepository.countOrderStatsByWeek(start, end, merchantId);
            case "month" -> travelOrderRepository.countOrderStatsByMonth(start, end, merchantId);
            default -> throw new IllegalArgumentException("不支持的 period 类型: " + period);
        };

        // 4. 构造图表数据（附加字段为 merchantId，可用于前端标记来源）
        String label = (merchantId != null && !merchantId.isEmpty()) ? merchantId : "ALL";
        return ChartDataUtil.buildDualChartDataY2(period, startDate, endDate, rawData);
    }

    public PageResponseDto<PackageSummaryDto> getTripRank(@Valid PageRequestDto pageRequestDto, String upperCase) {
        // 1. 排序字段映射，建议后期用枚举替代字符串
        String sortField = switch (upperCase) {
            case "VIEW" -> "viewCount";
            case "FAVORITE" -> "favoriteCount";
            case "COMMENT" -> "commentCount";
            case "SALES" -> "salesCount";
            case "HIDDEN_SCORE" -> "hiddenScore"; // 新增隐藏分排序
            case "AVERAGE_RATING" -> "averageRating"; // 新增平均评分排序
            case "COMPREHENSIVE" -> "hiddenScore"; // 综合排序默认用销量
            default -> throw new BizException("不支持的排行榜排序类型：" + upperCase);
        };

        // 2. 构建分页及排序
        Sort.Direction direction = Sort.Direction.fromString(pageRequestDto.getSortDirection());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), Sort.by(direction, sortField));

        // 3. 查询 PUBLISHED 状态的旅行团
        Page<TravelPackage> page = travelPackageRepository.findByStatus(TravelPackage.PackageStatus.PUBLISHED, pageable);

        // 4. 转换实体到 DTO
        List<PackageSummaryDto> summaryList = page.getContent().stream()
                .map(dtoConverter::convertPackageToSummaryDto)
                .toList();

        // 5. 封装分页响应并返回
        return PageResponseDto.<PackageSummaryDto>builder()
                .content(summaryList)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }

    private static final String REDIS_KEY_K = "hidden_score:k";
    private static final double FALLBACK_K = 1000.0;
    private static final double LAMBDA = 0.03; // 时间衰减参数

    /**
     * 每天凌晨 2 点更新归一化参数 k（使用 P95 分位数）
     */
    @Scheduled(cron = "0 50 14 * * ?")
    public void updateKValue() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("✅ Redis PING: {}", pong);
        } catch (Exception e) {
            log.error("❌ Redis 健康检查失败", e);
        }
        List<Double> rawScores = travelPackageRepository.findAll().stream()
                .map(p -> 0.5 * p.getFavoriteCount() + 0.3 * p.getCommentCount() + 0.2 * p.getViewCount())
                .sorted()
                .collect(Collectors.toList());

        if (rawScores.isEmpty()) return;

        int index = (int) Math.ceil(0.95 * rawScores.size()) - 1;
        double p95 = rawScores.get(Math.max(0, index));
        double k = Math.max(FALLBACK_K, p95);

        redisTemplate.opsForValue().set(REDIS_KEY_K, String.valueOf(k));
    }

    /**
     * 每天凌晨 2:10 执行评分刷新
     */
    @Scheduled(cron = "0 51 14 * * ?")
    public void updateHiddenScores() {
        double k = getKFromCache();
        List<TravelPackage> packages = travelPackageRepository.findAll();

        for (TravelPackage p : packages) {
            p.recalculateHiddenScore(LAMBDA, k);
        }

        travelPackageRepository.saveAll(packages);
        log.info("✅ 已更新所有旅行团的综合评分。");
    }

    private double getKFromCache() {
        try {
            String value = redisTemplate.opsForValue().get(REDIS_KEY_K);
            return value != null ? Math.max(FALLBACK_K, Double.parseDouble(value)) : FALLBACK_K;
        } catch (NumberFormatException e) {
            return FALLBACK_K;
        }
    }

    /**
     * 每天凌晨 2:20 重新计算每个旅行团的平均评分
     */
    @Scheduled(cron = "0 22 14 * * ?")
    public void updateAllAverageRatings() {
        List<TravelPackage> packages = travelPackageRepository.findAll();

        for (TravelPackage travelPackage : packages) {
            List<PackageComment> comments = packageCommentRepository.findByTravelPackageAndParentIsNull(travelPackage);

            if (!comments.isEmpty()) {
                double avg = comments.stream()
                        .mapToInt(PackageComment::getRating)
                        .average()
                        .orElse(0.0);

                travelPackage.setAverageRating(round(avg, 1)); // 保留1位小数
            } else {
                travelPackage.setAverageRating(0.0);
            }
        }

        travelPackageRepository.saveAll(packages);
        log.info("✅ 已更新所有旅行团的大众评分。");
    }

    private double round(double value, int digits) {
        return Math.round(value * Math.pow(10, digits)) / Math.pow(10, digits);
    }

    @Scheduled(cron = "0 16 15 * * ?") // 每小时刷新一次
    @Transactional
    public void refreshMerchantAverageRatings() {
        // 获取所有商家
        List<Merchant> merchants = merchantRepository.findAll();

        for (Merchant merchant : merchants) {
            // 获取商家所有已发布的旅行团
            List<TravelPackage> packages = travelPackageRepository
                    .findByDealerAndStatus(merchant, TravelPackage.PackageStatus.PUBLISHED);

            // 计算平均评分
            double avg = packages.isEmpty() ? 0.0 :
                    packages.stream()
                            .mapToDouble(TravelPackage::getAverageRating)
                            .average()
                            .orElse(0.0);

            // 更新商家评分
            merchant.setAverageRating(avg);
        }

        merchantRepository.saveAll(merchants);
        log.info("✅ 已更新所有商家的大众评分。");
    }

    private MerchantrankDto toDto(Merchant merchant) {
        MerchantrankDto dto = new MerchantrankDto();
        dto.setId(merchant.getId());
        dto.setName(merchant.getUsername());
        dto.setAvatarUrl(AliyunOssUtil.generatePresignedGetUrl(merchant.getAvatarUrl(), 36000, IMAGE_PROCESS)); // 假设继承自 Account
        dto.setAverageRating(merchant.getAverageRating());
        return dto;
    }

    public PageResponseDto<MerchantrankDto> getMerchantRank(@Valid PageRequestDto dto) {
        // 排序
        Sort.Direction direction = Sort.Direction.fromString(dto.getSortDirection());
        Pageable pageable = PageRequest.of(dto.getPage() - 1, dto.getSize(), Sort.by(direction, "averageRating"));

        // 仅查已审核通过的商家
        Page<Merchant> page = merchantRepository.findByApproval(Merchant.status.APPROVED, pageable);

        // 转换为 DTO
        List<MerchantrankDto> content = page.getContent().stream().map(this::toDto).toList();

        return PageResponseDto.<MerchantrankDto>builder()
                .content(content)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .numberOfElements(page.getNumberOfElements())
                .build();
    }

    /**
     * 获取转化漏斗数据
     *
     * @param period          时间周期（day/week/month） - 这里可根据需要扩展，当前仅用于时间范围默认值计算
     * @param startDate       开始时间
     * @param endDate         结束时间
     * @param merchantId      当前商户ID，限定数据范围
     * @param travelPackageId 旅游团ID
     * @return key-value map，包含各个阶段的数量
     */
    public Map<String, Object> getConversionFunnel(String period, LocalDate startDate, LocalDate endDate,
                                                   String merchantId, String travelPackageId) {

        // 1. 统一计算时间范围，支持默认值和校验
        Pair<LocalDate, LocalDate> range = ChartDataUtil.resolveRange(period, startDate, endDate);
        startDate = range.getLeft();
        endDate = range.getRight();

        // 2. 统一转换为 LocalDateTime 左闭右开区间：[startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
        Pair<LocalDateTime, LocalDateTime> dateTimeRange = ChartDataUtil.toDateTimeRange(startDate, endDate);
        LocalDateTime start = dateTimeRange.getLeft();
        LocalDateTime end = dateTimeRange.getRight();

        // 3. 根据travelPackageId是否为空，分别调用不同的方法查询数据
        long viewCount;
        long favoriteCount;
        long commentCount;
        long joinCount;

        if (travelPackageId == null || travelPackageId.isBlank()) {
            // 统计该商家全部旅行团行为数据汇总
            viewCount = travelDepartureRepository.sumViewCountByMerchantAndPeriod(merchantId, start, end);
            favoriteCount = travelDepartureRepository.sumFavoriteCountByMerchantAndPeriod(merchantId, start, end);
            commentCount = travelDepartureRepository.sumCommentCountByMerchantAndPeriod(merchantId, start, end);
            joinCount = travelOrderRepository.countJoinCountByMerchantAndPeriod(merchantId, start, end);
        } else {
            // 只统计指定旅行团的数据
            viewCount = travelDepartureRepository.sumViewCountByPackageAndPeriod(travelPackageId, start, end);
            favoriteCount = travelDepartureRepository.sumFavoriteCountByPackageAndPeriod(travelPackageId, start, end);
            commentCount = travelDepartureRepository.sumCommentCountByPackageAndPeriod(travelPackageId, start, end);
            joinCount = travelOrderRepository.countJoinCountByPackageAndPeriod(travelPackageId, start, end);
        }

        // 4. 组装结果，结构清晰
        Map<String, Object> funnelData = new LinkedHashMap<>();
        funnelData.put("viewCount", viewCount);
        funnelData.put("favoriteCount", favoriteCount);
        funnelData.put("commentCount", commentCount);
        funnelData.put("joinCount", joinCount);

        // 5. 附加信息
        funnelData.put("period", period);
        funnelData.put("startDate", startDate.toString());
        funnelData.put("endDate", endDate.toString());
        funnelData.put("travelPackageId", travelPackageId == null ? "ALL" : travelPackageId);

        return funnelData;
    }
}

//    public Map<String, Object> getUserGrowthData(String period) {
//        Map<String, Object> result = new LinkedHashMap<>();
//        List<Object[]> rawData = accountRepository.getUserGrowthGroupedByPeriod(period);
//        for (Object[] row : rawData) {
//            result.put((String) row[0], row[1]);
//        }
//        return result;
//    }
//
//    public List<TripRankDTO> getPopularTrips(int topN) {
//        return tripRepository.findTopTripsByOrderCount(topN).stream()
//                .map(obj -> new TripRankDTO((String) obj[0], (String) obj[1], ((Number) obj[2]).intValue()))
//                .collect(Collectors.toList());
//    }
//
//    public Map<String, Object> getTripGrowthData(String period) {
//        Map<String, Object> result = new LinkedHashMap<>();
//        List<Object[]> rawData = tripRepository.getTripGrowthGroupedByPeriod(period);
//        for (Object[] row : rawData) {
//            result.put((String) row[0], row[1]);
//        }
//        return result;
//    }
//
//    public Map<String, Object> getOrderStats(String period) {
//        Map<String, Object> result = new LinkedHashMap<>();
//        List<Object[]> rawData = orderRepository.getOrderStatsGroupedByPeriod(period);
//        for (Object[] row : rawData) {
//            Map<String, Object> item = new HashMap<>();
//            item.put("count", row[1]);
//            item.put("revenue", row[2]);
//            result.put((String) row[0], item);
//        }
//        return result;
//    }
//
//    public List<TripRatingRankDTO> getTripRatingRank(int topN) {
//        return ratingRepository.findTopTripsByAvgRating(topN).stream()
//                .map(obj -> new TripRatingRankDTO((String) obj[0], (String) obj[1], ((Number) obj[2]).doubleValue()))
//                .collect(Collectors.toList());
//    }
//
//    public Map<String, Object> getMerchantRatingStats() {
//        Map<String, Object> result = new LinkedHashMap<>();
//        List<Object[]> rawData = ratingRepository.findAvgRatingByMerchant();
//        for (Object[] row : rawData) {
//            result.put((String) row[0], row[1]);
//        }
//        return result;
//    }

