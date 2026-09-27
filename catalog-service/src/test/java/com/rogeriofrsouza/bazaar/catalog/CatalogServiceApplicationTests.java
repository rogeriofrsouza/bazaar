package com.rogeriofrsouza.bazaar.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
@ActiveProfiles("local")
class CatalogServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
