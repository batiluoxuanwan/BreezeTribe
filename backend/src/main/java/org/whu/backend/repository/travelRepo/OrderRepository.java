package org.whu.backend.repository.travelRepo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.Order;

public interface OrderRepository extends JpaRepository<Order, String> {

}
