package com.rogeriofrsouza.bazaar.catalog;

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
class CatalogSecurityTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void readsArePublic() {
        assertThat(mvc.get().uri("/api/categories")).hasStatusOk();
        assertThat(mvc.get().uri("/api/products")).hasStatusOk();
    }

    @Test
    void creatingProductNeedsToken() {
        assertThat(mvc.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void creatingProductWithTokenReachesController() {
        assertThat(mvc.post().uri("/api/products").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    // No such endpoint yet: a write added to a public resource is protected by default.
    @Test
    void writesOnPublicResourcesNeedToken() {
        assertThat(mvc.post().uri("/api/categories")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.delete().uri("/api/products/{id}", UUID.randomUUID())).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apiDocsArePublic() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk();
    }
}
