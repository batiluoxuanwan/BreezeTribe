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
     * 查询所有状态为“待支付”且创建时间早于指定时间的订单
     * @param status 订单状态 (PENDING_PAYMENT)
     * @param time   时间点
     * @return 符合条件的订单列表
     */
    List<TravelOrder> findAllByStatusAndCreatedTimeBefore(TravelOrder.OrderStatus status, LocalDateTime time);

    /**
     * ====== 1. 按日统计（YYYY-MM-DD）======
     */
    @Query("""
    SELECT FUNCTION('DATE', o.createdTime), SUM(o.travelerCount), SUM(o.totalPrice)
    FROM TravelOrder o
    WHERE o.createdTime >= :start AND o.createdTime < :end
    AND (:merchantId IS NULL OR o.travelDeparture.travelPackage.dealer.id = :merchantId)
    GROUP BY FUNCTION('DATE', o.createdTime)
    ORDER BY FUNCTION('DATE', o.createdTime)
""")
    List<Object[]> countOrderStatsByDay(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("merchantId") String merchantId);

    /**
     * ====== 2. 按周统计（YEARWEEK，格式为202527）======
     */
    @Query("""
SELECT
  FUNCTION('YEARWEEK', o.createdTime, 1) AS yearWeek,
  SUM(o.travelerCount),
  SUM(o.totalPrice)
FROM TravelOrder o
JOIN o.travelDeparture d
JOIN d.travelPackage p
JOIN p.dealer dealer
WHERE o.createdTime >= :start AND o.createdTime < :end
  AND (:merchantId IS NULL OR dealer.id = :merchantId)
GROUP BY FUNCTION('YEARWEEK', o.createdTime, 1)
ORDER BY FUNCTION('YEARWEEK', o.createdTime, 1)
""")
    List<Object[]> countOrderStatsByWeek(
            @Param("start")      LocalDateTime start,
            @Param("end")        LocalDateTime end,
            @Param("merchantId") String merchantId
    );

    /**
     * ====== 3. 按月统计（YYYY-MM）======
     */
    @Query("""
    SELECT FUNCTION('DATE_FORMAT', o.createdTime, '%Y-%m'),SUM(o.travelerCount), SUM(o.totalPrice)
    FROM TravelOrder o
    WHERE o.createdTime >= :start AND o.createdTime < :end
    AND (:merchantId IS NULL OR o.travelDeparture.travelPackage.dealer.id = :merchantId)
    GROUP BY FUNCTION('DATE_FORMAT', o.createdTime, '%Y-%m')
    ORDER BY FUNCTION('DATE_FORMAT', o.createdTime, '%Y-%m')
""")
    List<Object[]> countOrderStatsByMonth(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("merchantId") String merchantId);

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

    /**
     * /*
     * 【新增】根据团期ID，分页查询订单
     */
    Page<TravelOrder> findByTravelDeparture_Id(String departureId, Pageable pageable);

    @Query("""
    SELECT COALESCE(SUM(o.travelerCount), 0)
    FROM TravelOrder o
    WHERE o.travelDeparture.travelPackage.dealer.id = :merchantId
      AND o.createdTime >= :start
      AND o.createdTime < :end
      AND o.status = 'PAID'
""")
    Long countJoinCountByMerchantAndPeriod(@Param("merchantId") String merchantId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    @Query("""
    SELECT COALESCE(SUM(o.travelerCount), 0)
    FROM TravelOrder o
    WHERE o.travelDeparture.travelPackage.id = :packageId
      AND o.createdTime >= :start
      AND o.createdTime < :end
      AND o.status = 'PAID'
""")
    Long countJoinCountByPackageAndPeriod(@Param("packageId") String packageId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    List<TravelOrder> findAllByUserIdAndStatusOrderByUpdatedTimeDesc(String currentUserId, TravelOrder.OrderStatus orderStatus);
}
