package org.whu.backend.service.merchant;

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
import org.whu.backend.dto.order.TravelOrderDetailDto;
import org.whu.backend.dto.travelpack.DepartureCreateDto;
import org.whu.backend.dto.travelpack.DepartureSummaryDto;
import org.whu.backend.dto.travelpack.DepartureUpdateDto;
import org.whu.backend.entity.travelpac.TravelDeparture;
import org.whu.backend.entity.travelpac.TravelOrder;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.repository.travelRepo.TravelDepartureRepository;
import org.whu.backend.repository.travelRepo.TravelOrderRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.whu.backend.service.DtoConverter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MerchantDepartureService {

    @Autowired
    private TravelPackageRepository packageRepository;

    @Autowired
    private TravelDepartureRepository departureRepository;
    @Autowired
    private DtoConverter dtoConverter;
    @Autowired
    private TravelOrderRepository travelOrderRepository;

    @Transactional
    public List<TravelDeparture> createDepartures(String packageId, List<DepartureCreateDto> createDTOs, String currentDealerId) {
        // 1. 验证产品是否存在且属于当前经销商
//        log.error(createDTOs.get(0).getDepartureDate().toString());
        TravelPackage travelPackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new BizException("找不到对应的旅游产品"));

        if (!travelPackage.getDealer().getId().equals(currentDealerId)) {
            throw new BizException("无权操作不属于自己的产品");
        }

        // 2. 批量创建团期
        List<TravelDeparture> newDepartures = createDTOs.stream().map(dto -> {
            // 可以在此添加业务逻辑，如检查出发日期是否在未来
            if (dto.getDepartureDate().isBefore(LocalDateTime.now())) {
                throw new BizException("出发日期不能是过去的时间");
            }

            TravelDeparture departure = new TravelDeparture();
            departure.setTravelPackage(travelPackage);
            departure.setDepartureDate(dto.getDepartureDate().truncatedTo(ChronoUnit.DAYS)); // 截断到天
            departure.setPrice(dto.getPrice());
            departure.setCapacity(dto.getCapacity());
            departure.setStatus(TravelDeparture.DepartureStatus.OPEN); // 默认状态为开放报名
            departure.setParticipants(0); // 初始报名人数为0
            return departure;
        }).collect(Collectors.toList());

        // 3. 保存到数据库
        return departureRepository.saveAll(newDepartures);
    }

    @Transactional
    public TravelDeparture updateDeparture(String departureId, DepartureUpdateDto updateDTO, String currentDealerId) {
        // 1. 验证团期是否存在且属于当前经销商
        TravelDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new BizException("找不到对应的团期"));

        if (!departure.getTravelPackage().getDealer().getId().equals(currentDealerId)) {
            throw new BizException("无权操作不属于自己的团期");
        }

        // 2. 添加业务规则检查
        if (departure.getDepartureDate().isBefore(LocalDateTime.now())) {
            throw new BizException("不能修改已过期的团期");
        }
        if (departure.getParticipants() > 0 && updateDTO.getCapacity() != null && updateDTO.getCapacity() < departure.getParticipants()) {
            throw new BizException("团期容量不能小于已报名人数");
        }

        // 3. 更新字段
        if (updateDTO.getPrice() != null) {
            departure.setPrice(updateDTO.getPrice());
        }
        if (updateDTO.getCapacity() != null) {
            departure.setCapacity(updateDTO.getCapacity());
        }

        return departureRepository.save(departure);
    }

    @Transactional
    public void deleteDeparture(String departureId, String currentDealerId) {
        // 1. 验证团期是否存在且属于当前经销商
        TravelDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new BizException("找不到对应的团期"));

        if (!departure.getTravelPackage().getDealer().getId().equals(currentDealerId)) {
            throw new BizException("无权操作不属于自己的团期");
        }

        // 2. 【核心业务规则】检查是否已有用户报名
        if (departure.getParticipants() > 0) {
            throw new BizException("该团期已有用户报名，无法删除");
        }

        // 3. 执行删除
        departureRepository.delete(departure);
    }


    /**
     * 分页查询指定产品下的团期列表，支持按状态筛选
     */
    @Transactional(readOnly = true)
    public PageResponseDto<DepartureSummaryDto> getDeparturesForPackage(String packageId, String currentDealerId, String status, PageRequestDto pageRequestDto) {
        log.info("服务层：开始为经销商ID '{}' 查询产品ID '{}' 的团期列表，状态: {}", currentDealerId, packageId, status);

        // 1. 验证产品是否存在且属于当前经销商
        if (!packageRepository.existsByIdAndDealerId(packageId, currentDealerId)) {
            throw new BizException("找不到对应的旅游产品或无权访问");
        }

        // 2. 创建分页请求
        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        // 3. 根据前端传入的status，决定调用哪个查询方法
        Page<TravelDeparture> departurePage;
        LocalDateTime now = LocalDateTime.now(); // 获取当前时间作为分割点

        if ("UPCOMING".equalsIgnoreCase(status)) {
            // 查询“待出发”的团期（出发时间在当前时间之后）
            log.info("服务层：筛选条件为“待出发”，查询 {} 之后的团期。", now);
            departurePage = departureRepository.findByTravelPackageIdAndDepartureDateAfter(packageId, now, pageable);
        } else if ("PAST".equalsIgnoreCase(status)) {
            // 查询“已出发”的团期（出发时间在当前时间或之前）
            log.info("服务层：筛选条件为“已出发”，查询 {} 之前的团期。", now);
            departurePage = departureRepository.findByTravelPackageIdAndDepartureDateBefore(packageId, now, pageable);
        } else {
            // 如果不提供status，或status为其他值，则返回所有团期
            log.info("服务层：无状态筛选条件，查询所有团期。");
            departurePage = departureRepository.findByTravelPackageId(packageId, pageable);
        }

        log.info("服务层：查询到 {} 条团期数据。", departurePage.getTotalElements());

        // 5. 封装并返回分页DTO
        return dtoConverter.convertPageToDto(departurePage, dtoConverter::convertDepartureToSummaryDto);
    }

    /**
     * 获取指定团期的已支付订单列表（分页）
     */
    @Transactional(readOnly = true)
    public PageResponseDto<TravelOrderDetailDto> getOrdersForDeparture(String departureId, String currentDealerId, PageRequestDto pageRequestDto) {
        log.info("服务层：经销商ID '{}' 正在获取团期ID '{}'的订单列表, 分页参数: {}", currentDealerId, departureId, pageRequestDto);

        // 1. 验证该团期是否存在，并且是否属于当前经销商
        findDepartureByIdAndVerifyOwnership(departureId, currentDealerId);

        // 2. 【核心改变】构建一个包含多重排序规则的Sort对象
        // a. 主排序规则：按照我们新增的 statusPriority 字段升序排
        Sort primarySort = Sort.by(Sort.Direction.ASC, "statusPriority");
        // b. 次排序规则：使用前端传入的排序规则
        Sort secondarySort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        // c. 将两个排序规则合并起来
        Sort finalSort = primarySort.and(secondarySort);
        // 3. 创建分页对象，并传入我们全新的、强大的排序规则
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), finalSort);

        // 4. 查询逻辑变更：现在查询所有状态的订单
        Page<TravelOrder> orderPage = travelOrderRepository.findByTravelDeparture_Id(departureId, pageable);
        log.info("服务层：为团期ID '{}' 查询到 {} 条订单", departureId, orderPage.getTotalElements());

        // 5. 将实体列表转换为DTO列表（逻辑不变）
        return dtoConverter.convertPageToDto(orderPage,dtoConverter::convertTravelOrderToDetailDto);
    }

    // 一个私有辅助方法，用于查找旅行团并验证所有权
    private TravelDeparture findDepartureByIdAndVerifyOwnership(String departureId, String currentDealerId) {

        TravelDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new BizException("找不到对应的团期"));

        if (!departure.getTravelPackage().getDealer().getId().equals(currentDealerId)) {
            throw new BizException("无权操作不属于自己的团期");
        }
        return departure;
    }
}
