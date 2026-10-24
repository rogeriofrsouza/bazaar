package com.rogeriofrsouza.bazaar.order.inventory;

import com.rogeriofrsouza.bazaar.order.inventory.ReservationRequest.ReservationItemRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryClientTests {

    private static final String RESERVATIONS = "http://inventory-service/api/inventory/reservations";

    private MockRestServiceServer server;
    private InventoryClient inventoryClient;

    @BeforeEach
    void setUp() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        // Pass requests straight through instead of resolving inventory-service through Eureka.
        DeferringLoadBalancerInterceptor interceptor =
                mock(DeferringLoadBalancerInterceptor.class, Answers.CALLS_REAL_METHODS);
        willAnswer(invocation -> invocation
                .<ClientHttpRequestExecution>getArgument(2)
                .execute(invocation.getArgument(0), invocation.getArgument(1)))
                .given(interceptor).intercept(any(), any(), any());

        inventoryClient = new InventoryClient(builder, interceptor);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reservesWithCallersToken() {
        Jwt jwt = Jwt.withTokenValue("customer-token").header("alg", "none").subject("subject-1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        UUID productId = UUID.randomUUID();
        server.expect(requestTo(RESERVATIONS))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer customer-token"))
                .andExpect(content().json("""
                        {"items": [{"productId": "%s", "quantity": 2}]}
                        """.formatted(productId)))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        inventoryClient.reserve(new ReservationRequest(List.of(new ReservationItemRequest(productId, 2))));

        server.verify();
    }

    @Test
    void releasesReservation() {
        server.expect(requestTo(RESERVATIONS + "/release"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        inventoryClient.release(new ReservationRequest(List.of(new ReservationItemRequest(UUID.randomUUID(), 1))));

        server.verify();
    }

    @Test
    void failsWithConflictWhenStockIsShort() {
        server.expect(requestTo(RESERVATIONS))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"status": 409, "detail": "Only 1 units of keyboard available"}
                                """));

        assertThatThrownBy(() -> inventoryClient.reserve(reservation()))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).isEqualTo("Only 1 units of keyboard available");
                });
    }

    @Test
    void failsWithConflictWhenProductHasNoInventory() {
        server.expect(requestTo(RESERVATIONS))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> inventoryClient.reserve(reservation()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void failsWhenInventoryFails() {
        server.expect(requestTo(RESERVATIONS))
                .andRespond(withServerError());

        assertThatThrownBy(() -> inventoryClient.reserve(reservation()))
                .isInstanceOf(InventoryUnavailableException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }

    private static ReservationRequest reservation() {
        return new ReservationRequest(List.of(new ReservationItemRequest(UUID.randomUUID(), 1)));
    }
}
