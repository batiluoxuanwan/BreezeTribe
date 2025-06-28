package org.whu.backend.repository.travelRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.Order;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.accounts.User;

public interface OrderRepository extends JpaRepository<Order, String> {
    // 根据旅行团ID和订单状态分页查询订单
    Page<Order> findByTravelPackageIdAndStatus(String packageId, Order.OrderStatus status, Pageable pageable);


    boolean existsByUserAndTravelPackageAndStatus(User author, TravelPackage travelPackage, Order.OrderStatus orderStatus);
}
