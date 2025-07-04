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

    /**
     * 【新增】查询在指定时间段内出发的所有团期（用于通知商家）
     */
    @Query("SELECT d FROM TravelDeparture d WHERE d.departureDate BETWEEN :start AND :end")
    List<TravelDeparture> findDeparturesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
