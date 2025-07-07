package org.whu.backend.controller.merchant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.whu.backend.common.Result;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.PageRequestDto;
import org.whu.backend.dto.PageResponseDto;
import org.whu.backend.dto.order.TravelOrderDetailDto;
import org.whu.backend.dto.travelpack.DepartureCreateDto;
import org.whu.backend.dto.travelpack.DepartureSummaryDto;
import org.whu.backend.dto.travelpack.DepartureUpdateDto;
import org.whu.backend.entity.travelpac.TravelDeparture;
import org.whu.backend.repository.travelRepo.TravelOrderRepository;
import org.whu.backend.service.DtoConverter;
import org.whu.backend.service.merchant.MerchantDepartureService;
import org.whu.backend.util.AccountUtil;

import java.util.List;

@Tag(name = "经销商-团期管理", description = "经销商管理其旅游产品下的具体出发团期")
@RestController
@Slf4j
@RequestMapping("/api/merchant") // Grouping under a common path
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantDepartureController {

    @Autowired
    private MerchantDepartureService merchantDepartureService;

    @Autowired
    private DtoConverter dtoConverter;
    @Autowired
    private TravelOrderRepository travelOrderRepository;

    @Operation(summary = "为产品批量添加出发团期", description = "为一个产品模板一次性添加一个或多个出发日期、价格和库存。")
    @PostMapping("/packages/{packageId}/departures")
    public Result<List<DepartureSummaryDto>> createDepartures(
            @PathVariable String packageId,
            @Valid @RequestBody List<DepartureCreateDto> createDTOs) {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("经销商ID '{}' 正在为产品ID '{}' 批量添加 {} 个团期", currentDealerId, packageId, createDTOs.size());

        List<TravelDeparture> createdDepartures = merchantDepartureService.createDepartures(packageId, createDTOs, currentDealerId);

        List<DepartureSummaryDto> dtos = createdDepartures.stream()
                .map(dtoConverter::convertDepartureToSummaryDto).toList();

        return Result.success("团期添加成功", dtos);
    }

    @Operation(summary = "更新一个团期的信息", description = "只允许更新价格和库存。团期开始后或已有报名时可能限制更新。")
    @PutMapping("/departures/{departureId}")
    public Result<DepartureSummaryDto> updateDeparture(
            @PathVariable String departureId,
            @Valid @RequestBody DepartureUpdateDto updateDTO) {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("经销商ID '{}' 正在更新团期ID '{}'", currentDealerId, departureId);

        TravelDeparture updatedDeparture = merchantDepartureService.updateDeparture(departureId, updateDTO, currentDealerId);

        DepartureSummaryDto dto = dtoConverter.convertDepartureToSummaryDto(updatedDeparture);

        return Result.success("团期信息更新成功", dto);
    }

    @Operation(summary = "删除一个团期", description = "如果该团期已有用户报名，则不允许删除。")
    @DeleteMapping("/departures/{departureId}")
    public Result<?> deleteDeparture(@PathVariable String departureId) {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("经销商ID '{}' 正在删除团期ID '{}'", currentDealerId, departureId);

        boolean hasActiveOrders = travelOrderRepository.existsByTravelDepartureId(departureId);
        if (hasActiveOrders) {
            log.warn("删除失败：团期 '{}' 存在有效的关联团期。", departureId);
            throw new BizException("无法删除：该旅行团存在有效的关联订单，请先处理相关订单。");
        }

        merchantDepartureService.deleteDeparture(departureId, currentDealerId);

        return Result.success("团期删除成功");
    }

    @Operation(summary = "获取产品下的团期列表（分页）", description = "查询指定旅游产品模板下的所有出发团期。")
    @GetMapping("/packages/{packageId}/departures")
    public Result<PageResponseDto<DepartureSummaryDto>> getDeparturesForPackage(
            @Parameter(description = "产品模板的ID") @PathVariable String packageId,
            @Parameter(description = "按状态筛选（可选）：UPCOMING (待出发), PAST (已出发)。不传则返回所有。")
            @RequestParam(required = false) String status,
            @Valid @ParameterObject PageRequestDto pageRequestDto) {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("请求日志：经销商ID '{}' 正在查询产品ID '{}' 的团期列表, 分页参数: {}", currentDealerId, packageId, pageRequestDto);

        PageResponseDto<DepartureSummaryDto> resultPage = merchantDepartureService.getDeparturesForPackage(packageId, currentDealerId, status, pageRequestDto);

        return Result.success("团期列表查询成功", resultPage);
    }

    @Operation(summary = "【全新】查看指定团期的已支付订单列表（分页）", description = "让经销商了解自己某个具体团期的销售情况。")
    @GetMapping("/departures/{departureId}/orders")
    public Result<PageResponseDto<TravelOrderDetailDto>> getDepartureOrders(
            @Parameter(description = "要查询的团期ID") @PathVariable String departureId,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        String currentDealerId = AccountUtil.getCurrentAccountId();
        log.info("请求日志：经销商ID '{}' 正在查询团期ID '{}' 的订单列表", currentDealerId, departureId);

        PageResponseDto<TravelOrderDetailDto> resultPage = merchantDepartureService.getOrdersForDeparture(departureId, currentDealerId, pageRequestDto);

        return Result.success("订单列表查询成功", resultPage);
    }
}
