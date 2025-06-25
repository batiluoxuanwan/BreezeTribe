package org.whu.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.Route;

@Repository
public interface RouteRepository extends JpaRepository<Route, String> {
    Page<Route> findByDealerId(String dealerId, Pageable pageable);
}
