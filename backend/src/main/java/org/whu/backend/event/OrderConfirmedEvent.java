package org.whu.backend.event;

import lombok.Getter;
import org.whu.backend.entity.travelpac.TravelOrder;

@Getter // 使用Lombok
public class OrderConfirmedEvent {
    private final TravelOrder order;

    public OrderConfirmedEvent(TravelOrder order) {
        this.order = order;
    }
}