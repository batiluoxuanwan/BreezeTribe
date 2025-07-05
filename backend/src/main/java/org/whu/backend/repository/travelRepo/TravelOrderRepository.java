package org.whu.backend.repository.travelRepo;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.whu.backend.entity.travelpac.TravelOrder;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.entity.accounts.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface TravelOrderRepository extends JpaRepository<TravelOrder, String> {
    Page<TravelOrder> findByUserId(String currentUserId, Pageable pageable);

    Page<TravelOrder> findByUserIdAndStatusAndTravelDeparture_TravelPackage_IdIn(String currentUserId, TravelOrder.OrderStatus orderStatus, Set<String> reviewedPackageIds, Pageable pageable);

    Page<TravelOrder> findByUserIdAndStatus(String currentUserId, TravelOrder.OrderStatus orderStatus, Pageable pageable);

    Page<TravelOrder> findByUserIdAndStatusAndTravelDeparture_TravelPackage_IdNotIn(String currentUserId, TravelOrder.OrderStatus orderStatus, Set<String> reviewedPackageIds, Pageable pageable);

//    Page<TravelOrder> findByTravelDeparture_TravelPackage_IdAndStatus(String packageId, TravelOrder.OrderStatus orderStatus, Pageable pageable);

    Page<TravelOrder> findByTravelDeparture_TravelPackage_Id(String packageId, Pageable pageable);

    boolean existsByTravelDepartureId(String id);

    boolean existsByUserAndTravelDeparture_TravelPackageAndStatus(User author, TravelPackage travelPackage, TravelOrder.OrderStatus orderStatus);

    /**
     * 【新增】检查一个产品模板下是否存在任何活跃的订单（待支付或已支付）
     *
     * @param packageId 产品模板的ID
     * @return 如果存在则返回true，否则返回false
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
            "FROM TravelOrder o " +
            "WHERE o.travelDeparture.travelPackage.id = :packageId " +
            "AND o.status IN ('PENDING_PAYMENT', 'PAID')")
    boolean existsActiveOrdersForPackage(@Param("packageId") String packageId);
    // Spring Data JPA 也可以用更简洁的方法名派生查询来实现同样的效果：
    // boolean existsByTravelDeparture_TravelPackage_IdAndStatusIn(String packageId, List<TravelOrder.OrderStatus> statuses);


    /**
     * 【新增】查询一个用户购买过的所有旅游产品ID
     */
    @Query("SELECT DISTINCT o.travelDeparture.travelPackage.id FROM TravelOrder o WHERE o.user.id = :userId")
    Set<String> findPackageIdsByUserId(@Param("userId") String userId);

    /**
     * 【新增】将已支付(PAID)且已到出发日期的订单，状态更新为进行中(ONGOING)
     *
     * @param now 当前时间
     * @return 更新的记录数
     */
    @Modifying
    @Query(value = "UPDATE travel_orders o " +
            "JOIN travel_departures d ON o.departure_id = d.id " +
            "SET o.status = 'ONGOING' " +
            "WHERE o.status = 'PAID' AND d.departure_date <= :now",
            nativeQuery = true)
    int updatePaidToOngoing(@Param("now") LocalDateTime now);


    /**
     * 【新增】将进行中(ONGOING)且行程已结束的订单，状态更新为已完成(COMPLETED)
     * 由于JPQL标准不支持直接进行日期运算（如日期+天数），我们使用原生SQL查询以获得最佳的数据库兼容性和性能。
     *
     * @param now 当前时间
     * @return 更新的记录数
     */
    @Modifying
    @Query(value = "UPDATE travel_orders o " +
            "JOIN travel_departures d ON o.departure_id = d.id " +
            "JOIN travel_packages p ON d.package_id = p.id " +
            "SET o.status = 'COMPLETED' " +
            "WHERE o.status = 'ONGOING' AND DATE_ADD(d.departure_date, INTERVAL p.duration_in_days DAY) <= :now",
            nativeQuery = true)
    int updateOngoingToCompleted(@Param("now") LocalDateTime now);

    /**
     * 【新增】查询在指定时间段内出发的、已支付的订单（用于通知用户）
     */
    @Query("SELECT o FROM TravelOrder o WHERE o.status = 'PAID' AND o.travelDeparture.departureDate BETWEEN :start AND :end")
    List<TravelOrder> findPaidOrdersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 【新增】根据团期ID和订单状态，分页查询订单
     */
    Page<TravelOrder> findByTravelDeparture_IdAndStatus(String departureId, TravelOrder.OrderStatus status, Pageable pageable);
}
