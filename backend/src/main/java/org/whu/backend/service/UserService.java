package org.whu.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.whu.backend.common.Result;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.order.OrderCreateRequestDto;
import org.whu.backend.dto.order.OrderDetailDto;
import org.whu.backend.entity.Order;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.repository.OrderRepository;
import org.whu.backend.repository.TravelPackageRepository;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.util.SecurityUtil;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TravelPackageRepository travelPackageRepository;

    public OrderDetailDto createOrder(OrderCreateRequestDto orderCreateRequestDto) {
        // TODO: 业务逻辑
        // 1. 获取当前登录用户的ID
        String userId = SecurityUtil.getCurrentUserId();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new BizException("用户不存在");
        }
        User user = userOpt.get();
        // 2. 验证旅行团ID是否存在且可报名
        Optional<TravelPackage> travelPackageOpt = travelPackageRepository.findById(orderCreateRequestDto.getPackageId());
        if (travelPackageOpt.isEmpty()) {
            throw new BizException("旅行团不存在");
        }
        TravelPackage travelPackage = travelPackageOpt.get();
        // 3. (可选)检查库存或人数限制
        if (travelPackage.getStatus() != TravelPackage.PackageStatus.PUBLISHED) {
            throw new BizException("该旅行团目前不可报名");
        }
        Integer capacity = travelPackage.getCapacity();
        Integer participants = travelPackage.getParticipants() == null ? 0 : travelPackage.getParticipants();
        if (capacity != null && participants >= capacity) {
            throw new BizException("旅行团报名人数已满");
        }
        // 4. 创建新的Order实体并保存
        Order order = new Order();
        order.setUser(user);
        order.setTravelPackage(travelPackage);
        order.setTravelerCount(orderCreateRequestDto.getTravelerCount());
        order.setTotalPrice(travelPackage.getPrice().multiply(BigDecimal.valueOf(orderCreateRequestDto.getTravelerCount())));
        order.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        orderRepository.save(order);
        // 5. 返回创建成功的订单详情
        travelPackage.setParticipants(participants + order.getTravelerCount());
        travelPackageRepository.save(travelPackage);
        // 7. 返回订单详情DTO
        //TODO:
        return new OrderDetailDto();
    }
}
