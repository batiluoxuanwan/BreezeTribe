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
     * 分页查询指定产品下的团期列表
     */
    @Transactional(readOnly = true)
    public PageResponseDto<DepartureSummaryDto> getDeparturesForPackage(String packageId, String currentDealerId, PageRequestDto pageRequestDto) {
        log.info("服务层：开始为经销商ID '{}' 查询产品ID '{}' 的团期列表", currentDealerId, packageId);

        // 1. 验证产品是否存在且属于当前经销商
        if (!packageRepository.existsByIdAndDealerId(packageId, currentDealerId)) {
            throw new BizException("找不到对应的旅游产品或无权访问");
        }

        // 2. 创建分页请求
        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        // 3. 查询分页数据 (需要一个新Repository方法)
        Page<TravelDeparture> departurePage = departureRepository.findByTravelPackageId(packageId, pageable);

        // 4. 转换DTO
        List<DepartureSummaryDto> summaryDtos = departurePage.getContent().stream()
                .map(dtoConverter::convertDepartureToSummaryDto)
                .collect(Collectors.toList());

        log.info("查询到 {} 条团期数据", departurePage.getTotalElements());

        // 5. 封装并返回分页DTO
        return dtoConverter.convertPageToDto(departurePage, summaryDtos);
    }

    /**
     * 获取指定团期的已支付订单列表（分页）
     */
    @Transactional(readOnly = true)
    public PageResponseDto<TravelOrderDetailDto> getOrdersForDeparture(String departureId, String currentDealerId, PageRequestDto pageRequestDto) {
        log.info("服务层：经销商ID '{}' 正在获取团期ID '{}'的订单列表, 分页参数: {}", currentDealerId, departureId, pageRequestDto);

        // 1. 验证团期是否存在且属于当前经销商
        findDepartureByIdAndVerifyOwnership(departureId, currentDealerId);

        // 2. 创建分页和排序对象
        Sort sort = Sort.by(Sort.Direction.fromString(pageRequestDto.getSortDirection()), pageRequestDto.getSortBy());
        Pageable pageable = PageRequest.of(pageRequestDto.getPage() - 1, pageRequestDto.getSize(), sort);

        // 3. 【重大改变】查询逻辑变更
        // 现在我们精确地查询关联到这个departureId的、已支付的订单
        Page<TravelOrder> orderPage = travelOrderRepository.findByTravelDeparture_IdAndStatus(
                departureId,
                TravelOrder.OrderStatus.PAID, // 只查询已支付的订单
                pageable
        );
        log.info("服务层：为团期ID '{}' 查询到 {} 条已支付订单", departureId, orderPage.getTotalElements());

        // 4. 将实体列表转换为对经销商安全的DTO列表
        List<TravelOrderDetailDto> dtos = orderPage.getContent().stream()
                .map(dtoConverter::convertTravelOrderToDetailDto) // 使用新的转换方法
                .collect(Collectors.toList());

        // 5. 封装并返回分页结果
        return dtoConverter.convertPageToDto(orderPage, dtos);
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
