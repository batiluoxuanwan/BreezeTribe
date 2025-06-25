package org.whu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.Spot;

import java.util.Optional;

@Repository
public interface SpotRepository extends JpaRepository<Spot, String> {
    Optional<Spot> findByMapProviderUid(String uid);
}
