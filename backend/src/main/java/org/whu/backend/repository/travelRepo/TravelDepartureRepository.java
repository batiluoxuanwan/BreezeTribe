package org.whu.backend.repository.travelRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.TravelDeparture;

public interface TravelDepartureRepository extends JpaRepository<TravelDeparture, String> {

    // 分页查询某个团模板下的所有团期
    Page<TravelDeparture> findByTravelPackageId(String packageId, Pageable pageable);
}
