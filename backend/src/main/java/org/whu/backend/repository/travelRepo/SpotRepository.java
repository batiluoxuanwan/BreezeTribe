package org.whu.backend.repository.travelRepo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.Spot;

import java.util.Optional;

public interface SpotRepository extends JpaRepository<Spot, String> {
    Optional<Spot> findByMapProviderUid(String uid);
}
