package org.whu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.TravelPackage;

@Repository
public interface TravelPackageRepository extends JpaRepository<TravelPackage, String> {
}
