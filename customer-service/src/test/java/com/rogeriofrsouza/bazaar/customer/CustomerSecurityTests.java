package com.rogeriofrsouza.bazaar.customer;

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
class CustomerSecurityTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void readingProfileNeedsToken() {
        assertThat(mvc.get().uri("/api/customers/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void readingProfileWithTokenReachesController() {
        assertThat(mvc.get().uri("/api/customers/me").with(jwt())).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void readingAddressesNeedsToken() {
        assertThat(mvc.get().uri("/api/customers/me/addresses")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void readingAddressesWithTokenReachesController() {
        assertThat(mvc.get().uri("/api/customers/me/addresses").with(jwt())).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void apiDocsArePublic() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatusOk();
    }
}
