package org.whu.backend.repository.travelRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.whu.backend.entity.Order;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.TravelPackage;
import org.whu.backend.entity.accounts.User;

import java.time.LocalDateTime;
import java.util.Set;

public interface OrderRepository extends JpaRepository<Order, String> {
    // 根据旅行团ID和订单状态分页查询订单
    Page<Order> findByTravelPackageIdAndStatus(String packageId, Order.OrderStatus status, Pageable pageable);
    Page<Order> findByUser(User user, Pageable pageable);

    // 查询用户已完成的、且旅行团ID在指定列表中的订单（用于查询“已评价”）
    Page<Order> findByUserIdAndStatusAndTravelPackageIdIn(String userId, Order.OrderStatus status, Set<String> packageIds, Pageable pageable);

    // 查询用户已完成的、且旅行团ID不在指定列表中的订单（用于查询“待评价”）
    Page<Order> findByUserIdAndStatusAndTravelPackageIdNotIn(String userId, Order.OrderStatus status, Set<String> packageIds, Pageable pageable);

    // 查询用户已完成的、所有订单
    Page<Order> findByUserIdAndStatus(String userId, Order.OrderStatus status, Pageable pageable);

    // 分页查询某个用户的所有订单
    Page<Order> findByUserId(String userId, Pageable pageable);

    boolean existsByUserAndTravelPackageAndStatus(User author, TravelPackage travelPackage, Order.OrderStatus orderStatus);

    /**
     * [新增] 检查一个旅行团是否存在任何“有效”状态的订单
     * “有效”状态通常指：待支付、已支付、进行中
     * @param packageId 旅行团ID
     * @return 如果存在，返回true
     */
    boolean existsByTravelPackageId(String packageId);

    /**
     * 批量将已支付且已过出发日期的订单，状态更新为“进行中”
     * @param currentTime 当前时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE Order o SET o.status = 'ONGOING' WHERE o.status = 'PAID' AND o.travelPackage.departureDate <= :currentTime")
    int updatePaidToOngoing(LocalDateTime currentTime);

    /**
     * 批量将进行中的、且行程已结束的订单，状态更新为“已完成”
     * @param currentTime 当前时间
     * @return 更新的记录数
     */
    @Modifying
    @Query(value = "UPDATE orders o JOIN travel_packages tp ON o.package_id = tp.id " +
            "SET o.status = 'COMPLETED' " +
            "WHERE o.status = 'ONGOING' AND DATE_ADD(tp.departure_date, INTERVAL tp.duration_in_days DAY) <= :currentTime",
            nativeQuery = true)
    int updateOngoingToCompleted(LocalDateTime currentTime);

}
