package com.rogeriofrsouza.bazaar.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

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
    void reservingNeedsToken() {
        assertThat(mvc.post().uri("/api/inventory/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationOf(UUID.randomUUID())))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void reservingWithTokenReachesController() {
        assertThat(mvc.post().uri("/api/inventory/reservations")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationOf(UUID.randomUUID())))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void apiDocsArePublic() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk();
    }

    private static String reservationOf(UUID productId) {
        return """
                {"items": [{"productId": "%s", "quantity": 1}]}
                """.formatted(productId);
    }
}
