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
}
