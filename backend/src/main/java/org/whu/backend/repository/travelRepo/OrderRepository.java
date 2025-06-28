package org.whu.backend.repository.travelRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.Order;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.accounts.User;

import java.util.Set;

public interface OrderRepository extends JpaRepository<Order, String> {
    // 根据旅行团ID和订单状态分页查询订单
    Page<Order> findByTravelPackageIdAndStatus(String packageId, Order.OrderStatus status, Pageable pageable);

    // 查询用户已完成的、且旅行团ID在指定列表中的订单（用于查询“已评价”）
    Page<Order> findByUserIdAndStatusAndTravelPackageIdIn(String userId, Order.OrderStatus status, Set<String> packageIds, Pageable pageable);

    // 查询用户已完成的、且旅行团ID不在指定列表中的订单（用于查询“待评价”）
    Page<Order> findByUserIdAndStatusAndTravelPackageIdNotIn(String userId, Order.OrderStatus status, Set<String> packageIds, Pageable pageable);

    // 查询用户已完成的、所有订单
    Page<Order> findByUserIdAndStatus(String userId, Order.OrderStatus status, Pageable pageable);

    // 分页查询某个用户的所有订单
    Page<Order> findByUserId(String userId, Pageable pageable);

    boolean existsByUserAndTravelPackageAndStatus(User author, TravelPackage travelPackage, Order.OrderStatus orderStatus);
}
