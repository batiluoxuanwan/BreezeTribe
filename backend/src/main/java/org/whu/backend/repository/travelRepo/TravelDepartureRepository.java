package org.whu.backend.repository.travelRepo;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.whu.backend.entity.travelpac.TravelDeparture;

import java.time.LocalDateTime;
import java.util.List;

public interface TravelDepartureRepository extends JpaRepository<TravelDeparture, String> {

    // 分页查询某个团模板下的所有团期
    Page<TravelDeparture> findByTravelPackageId(String packageId, Pageable pageable);

    /**
     * 【核心】原子化地增加团期已报名人数，并同时检查库存
     * @return 返回受影响的行数。如果返回0，说明更新失败（库存不足）。
     */
    @Modifying
    @Query("UPDATE TravelDeparture d SET d.participants = d.participants + :num " +
            "WHERE d.id = :departureId AND d.participants + :num <= d.capacity")
    int addParticipantCount(@Param("departureId") String departureId, @Param("num") Integer num);

    /**
     * 【核心】原子化地减少团期已报名人数（用于取消订单）
     */
    @Modifying
    @Query("UPDATE TravelDeparture d SET d.participants = d.participants - :num WHERE d.id = :departureId")
    int subParticipantCount(@Param("departureId") String departureId, @Param("num") Integer num);

    Page<TravelDeparture> findByTravelPackageIdAndStatus(String packageId, TravelDeparture.DepartureStatus departureStatus, Pageable pageable);

    /**
     * 根据产品ID、团期状态和出发日期，分页查询可用的团期
     * @param packageId 产品ID
     * @param status 团期状态 (e.g., OPEN)
     * @param date 日期，只查询此日期之后的团期
     * @param pageable 分页信息
     * @return 团期分页结果
     */
    Page<TravelDeparture> findByTravelPackageIdAndStatusAndDepartureDateAfter(String packageId, TravelDeparture.DepartureStatus status, LocalDateTime date, Pageable pageable);

    boolean existsByTravelPackageId(String packageId);


    @Query("""
        SELECT FUNCTION('DATE', td.createdTime), COUNT(td)
        FROM TravelDeparture td
        WHERE td.createdTime BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE', td.createdTime)
        ORDER BY FUNCTION('DATE', td.createdTime)
        """)
    List<Object[]> countByDay(@Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Query("""
        SELECT FUNCTION('YEARWEEK', td.createdTime), COUNT(td)
        FROM TravelDeparture td
        WHERE td.createdTime BETWEEN :start AND :end
        GROUP BY FUNCTION('YEARWEEK', td.createdTime)
        ORDER BY FUNCTION('YEARWEEK', td.createdTime)
        """)
    List<Object[]> countByWeek(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', td.createdTime, '%Y-%m'), COUNT(td)
        FROM TravelDeparture td
        WHERE td.createdTime BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE_FORMAT', td.createdTime, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', td.createdTime, '%Y-%m')
        """)
    List<Object[]> countByMonth(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    /**
     * 【新增】查询在指定时间段内出发的所有团期（用于通知商家）
     */
    @Query("SELECT d FROM TravelDeparture d WHERE d.departureDate BETWEEN :start AND :end")
    List<TravelDeparture> findDeparturesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 【新增】将所有已过期（出发时间早于当前时间）且状态仍为OPEN或CLOSED的团期，
     * 批量更新其状态为FINISHED。
     * @param now 当前时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE TravelDeparture d SET d.status = 'FINISHED' " +
            "WHERE d.departureDate < :now AND d.status IN ('OPEN', 'CLOSED')")
    int updateStatusToFinishedForExpiredDepartures(@Param("now") LocalDateTime now);

    Page<TravelDeparture> findByTravelPackageIdAndDepartureDateAfter(String packageId, LocalDateTime now, Pageable pageable);

    Page<TravelDeparture> findByTravelPackageIdAndDepartureDateBefore(String packageId, LocalDateTime now, Pageable pageable);
}
