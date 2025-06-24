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
    }

}
