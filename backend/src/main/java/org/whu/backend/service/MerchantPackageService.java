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
import org.whu.backend.dto.travelpack.PackageCreateRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.dto.travelpack.PackageUpdateRequestDto;
import org.whu.backend.entity.Route;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.accounts.Merchant;
import org.whu.backend.entity.association.PackageRoute;
import org.whu.backend.repository.authRepo.MerchantRepository;
import org.whu.backend.repository.travelRepo.RouteRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class MerchantPackageService {

    @Autowired
    private TravelPackageRepository packageRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private PublicService publicService;

    @Transactional
    public TravelPackage createPackage(PackageCreateRequestDto dto, String dealerId) {
        log.info("经销商ID '{}' 正在创建新旅行团 '{}'...", dealerId, dto.getTitle());

        // 1. 查找经销商实体
        Merchant dealer = merchantRepository.findById(dealerId)
                .orElseThrow(() -> new BizException("找不到ID为 " + dealerId + " 的经销商"));

        // 2. 创建并填充旅行团基本信息
        TravelPackage newPackage = new TravelPackage();
        newPackage.setTitle(dto.getTitle());
        newPackage.setDetailedDescription(dto.getDetailedDescription());
        newPackage.setPrice(dto.getPrice());
        newPackage.setCapacity(dto.getCapacity());
        newPackage.setParticipants(0);
        newPackage.setDepartureDate(dto.getDepartureDate());
        newPackage.setDurationInDays(dto.getDurationInDays());
        newPackage.setDealer(dealer);
        newPackage.setStatus(TravelPackage.PackageStatus.PENDING_APPROVAL); // 初始状态为待审核


        // TODO: 旅行团图册处理关联还没实现

        // 3. 处理并关联路线
        for (int i = 0; i < dto.getRoutesIds().size(); i++) {
            String id = dto.getRoutesIds().get(i);
            Route route = routeRepository.findById(id)
                    .orElseThrow(() -> new BizException("找不到ID为 " + id + " 的路线"));
            // 关键权限校验！确保该路线属于当前经销商
            if (!route.getDealer().getId().equals(dealerId)) {
                log.error("权限拒绝：经销商ID '{}' 试图使用不属于自己的路线ID '{}'。", dealerId, route.getId());
                throw new BizException("不能使用不属于自己的路线来创建旅行团");
            }
            // 创建关联实体并设置顺序
            PackageRoute packageRoute = new PackageRoute();
            packageRoute.setTravelPackage(newPackage);
            packageRoute.setRoute(route);
            packageRoute.setDayNumber(i + 1);// 顺序从1开始
            newPackage.getRoutes().add(packageRoute);
        }

        // 4. 保存旅行团
        TravelPackage savedPackage = packageRepository.save(newPackage);
        log.info("新旅行团 '{}' (ID: {}) 已成功创建，状态为待审核。", savedPackage.getTitle(), savedPackage.getId());
        return savedPackage;
    }


    // 更新旅行团的核心业务逻辑
    @Transactional
    public TravelPackage updatePackage(String packageId, PackageUpdateRequestDto dto, String currentDealerId) {
        log.info("经销商ID '{}' 正在尝试更新旅行团ID '{}'...", currentDealerId, packageId);

        // 1. 查找并验证旅行团的所有权
        TravelPackage packageToUpdate = findPackageByIdAndVerifyOwnership(packageId, currentDealerId);

        // 2. 更新允许修改的字段
        packageToUpdate.setTitle(dto.getTitle());
        packageToUpdate.setDetailedDescription(dto.getDetailedDescription());

        // 3. 重要！每次修改后，都需要重新进入待审核状态
        packageToUpdate.setStatus(TravelPackage.PackageStatus.PENDING_APPROVAL);

        // 4. 保存更新
        TravelPackage updatedPackage = packageRepository.save(packageToUpdate);
        log.info("旅行团ID '{}' 已被成功更新，状态重置为待审核。", packageId);
        return updatedPackage;
    }

    // 删除自己的一个旅行团（逻辑删除）
    @Transactional
    public void deletePackage(String packageId, String currentDealerId) {
        log.info("经销商ID '{}' 正在尝试删除旅行团ID '{}'...", currentDealerId, packageId);
        // 调用之前写好的私有方法来查找并验证所有权
        TravelPackage packageToDelete = findPackageByIdAndVerifyOwnership(packageId, currentDealerId);

        // 执行逻辑删除
        packageRepository.delete(packageToDelete);
        log.info("旅行团ID '{}' 已被经销商ID '{}' 成功删除。", packageId, currentDealerId);
    }

    // 获取自己创建的旅行团列表（分页）
    @Transactional(readOnly = true)
    public PageResponseDto<PackageSummaryDto> getMyPackages(String currentDealerId, PageRequestDto pageRequestDto) {
        log.info("经销商ID '{}' 正在获取自己的旅行团列表, 分页参数: {}", currentDealerId, pageRequestDto);

        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        Page<TravelPackage> packagePage = packageRepository.findByDealerId(currentDealerId, pageable);

        // 使用Lambda表达式来调用另一个Service中的方法，或者直接改成
        List<PackageSummaryDto> summaryDtos = packagePage.getContent().stream()
                .map(entity -> publicService.convertPackageToSummaryDto(entity))
                .collect(Collectors.toList());

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


    // 一个私有辅助方法，用于查找旅行团并验证所有权
    private TravelPackage findPackageByIdAndVerifyOwnership(String packageId, String currentDealerId) {
        TravelPackage travelPackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new BizException("找不到ID为 " + packageId + " 的旅行团"));

        if (!travelPackage.getDealer().getId().equals(currentDealerId)) {
            log.error("权限拒绝：经销商ID '{}' 试图操作不属于自己的旅行团ID '{}'。", currentDealerId, packageId);
            throw new BizException("无权操作此旅行团");
        }
        return travelPackage;
    }
}
