package org.whu.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.whu.backend.entity.Spot;
import org.whu.backend.repository.SpotRepository;

@SpringBootTest
class BackendApplicationTests {

    @Autowired
    SpotRepository spotRepository;

    @Test
    void contextLoads() {
        Spot spot = new Spot();
        spot.setName("下北泽");
        spot.setCityName("东京市");
        spot.setDescription("111");
        spot.setLongitude(11.1);
        spot.setLatitude(11.1);
        spotRepository.save(spot);
    }

}
