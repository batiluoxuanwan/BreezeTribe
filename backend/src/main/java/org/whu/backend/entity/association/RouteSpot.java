package org.whu.backend.entity.association;

import jakarta.persistence.*;
import lombok.Data;
import org.whu.backend.entity.Route;
import org.whu.backend.entity.Spot;

@Data
@Entity
@Table(name = "route_spots")
public class RouteSpot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "spot_id")
    private Spot spot;

    // 用于表示景点在这条路线中的顺序
    @Column(name = "order_column")
    private int orderColumn;
}