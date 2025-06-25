package org.whu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.Order;
import org.whu.backend.entity.Route;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

}
