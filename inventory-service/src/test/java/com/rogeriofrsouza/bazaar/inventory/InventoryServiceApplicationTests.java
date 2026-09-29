package com.rogeriofrsouza.bazaar.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(ContainersConfig.class)
class InventoryServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
