package org.whu.backend.repository.travelRepo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.whu.backend.entity.travelpac.Route;

public interface RouteRepository extends JpaRepository<Route, String> {
    Page<Route> findByDealerId(String dealerId, Pageable pageable);
}
