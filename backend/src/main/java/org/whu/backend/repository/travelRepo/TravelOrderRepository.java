package org.whu.backend.repository.travelRepo;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * 【新增】查询一个用户购买过的所有旅游产品ID
     */
    @Query("SELECT DISTINCT o.travelDeparture.travelPackage.id FROM TravelOrder o WHERE o.user.id = :userId")
    Set<String> findPackageIdsByUserId(@Param("userId") String userId);

    /**
     * ====== 1. 按日统计（YYYY-MM-DD）======
     */
    @Query("""
    SELECT FUNCTION('DATE', o.createdTime), COUNT(o), SUM(o.totalPrice)
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
    SELECT FUNCTION('YEARWEEK', o.createdTime), COUNT(o), SUM(o.totalPrice)
    FROM TravelOrder o
    WHERE o.createdTime >= :start AND o.createdTime < :end
    AND (:merchantId IS NULL OR o.travelDeparture.travelPackage.dealer.id = :merchantId)
    GROUP BY FUNCTION('YEARWEEK', o.createdTime)
    ORDER BY FUNCTION('YEARWEEK', o.createdTime)
""")
    List<Object[]> countOrderStatsByWeek(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end,
                                         @Param("merchantId") String merchantId);

    /**
     * ====== 3. 按月统计（YYYY-MM）======
     */
    @Query("""
    SELECT FUNCTION('DATE_FORMAT', o.createdTime, '%Y-%m'), COUNT(o), SUM(o.totalPrice)
    FROM TravelOrder o
    WHERE o.createdTime >= :start AND o.createdTime < :end
    AND (:merchantId IS NULL OR o.travelDeparture.travelPackage.dealer.id = :merchantId)
    GROUP BY FUNCTION('DATE_FORMAT', o.createdTime, '%Y-%m')
    ORDER BY FUNCTION('DATE_FORMAT', o.createdTime, '%Y-%m')
""")
    List<Object[]> countOrderStatsByMonth(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("merchantId") String merchantId);
}
