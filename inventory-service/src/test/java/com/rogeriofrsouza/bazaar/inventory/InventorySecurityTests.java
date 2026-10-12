package com.rogeriofrsouza.bazaar.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class InventorySecurityTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void listingNeedsToken() {
        assertThat(mvc.get().uri("/api/inventory")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listingWithTokenReachesController() {
        assertThat(mvc.get().uri("/api/inventory").with(jwt())).hasStatusOk();
    }

    @Test
    void apiDocsArePublic() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk();
    }
}
