package org.whu.backend.event;

import lombok.Getter;
import org.whu.backend.entity.travelpac.TravelOrder;

@Getter // 使用Lombok
public class OrderCancelledEvent {
    private final TravelOrder order;

    public OrderCancelledEvent(TravelOrder order) {
        this.order = order;
    }
}