package com.rogeriofrsouza.bazaar.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
class OrderSecurityTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void readingOrderNeedsToken() {
        assertThat(mvc.get().uri("/api/orders/{id}", UUID.randomUUID())).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void readingOrderWithTokenReachesController() {
        assertThat(mvc.get().uri("/api/orders/{id}", UUID.randomUUID()).with(jwt())).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void listingOrdersNeedsToken() {
        assertThat(mvc.get().uri("/api/orders")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listingOrdersWithTokenReachesController() {
        assertThat(mvc.get().uri("/api/orders").with(jwt())).hasStatusOk();
    }

    @Test
    void confirmingOrderNeedsToken() {
        assertThat(mvc.post().uri("/api/orders/{id}/confirm", UUID.randomUUID())).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void confirmingOrderWithTokenReachesController() {
        assertThat(mvc.post().uri("/api/orders/{id}/confirm", UUID.randomUUID()).with(jwt()))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void apiDocsArePublic() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk();
    }
}
