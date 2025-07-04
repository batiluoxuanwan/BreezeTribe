package org.whu.backend.entity.association;

import jakarta.persistence.*;
import lombok.Data;
import org.whu.backend.entity.travelpac.Route;
import org.whu.backend.entity.travelpac.TravelPackage;

@Data
@Entity
@Table(name = "package_routes")
public class PackageRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "package_id")
    private TravelPackage travelPackage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    private Route route;

    // 用于表示这是第几天的行程
    @Column(name = "day_number")
    private int dayNumber;
}
