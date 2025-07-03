package org.whu.backend.event;

import lombok.Getter;
import org.whu.backend.entity.travelpac.TravelOrder;

@Getter // 使用Lombok
public class OrderCreatedEvent {
    private final TravelOrder order;

    public OrderCreatedEvent(TravelOrder order) {
        this.order = order;
    }
}