package com.rogeriofrsouza.bazaar.order.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogClientTests {

    private MockRestServiceServer server;
    private CatalogClient catalogClient;

    @BeforeEach
    void setUp() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        // Pass requests straight through instead of resolving catalog-service through Eureka.
        DeferringLoadBalancerInterceptor interceptor = mock(DeferringLoadBalancerInterceptor.class);
        given(interceptor.intercept(any(), any(), any())).willAnswer(invocation -> invocation
                .<ClientHttpRequestExecution>getArgument(2)
                .execute(invocation.getArgument(0), invocation.getArgument(1)));

        catalogClient = new CatalogClient(builder, interceptor);
    }

    @Test
    void readsProductsFromPagedResponse() {
        server.expect(requestTo("http://catalog-service/api/products?codes=AAAA2222&size=1"))
                .andRespond(withSuccess("""
                        {
                          "content": [
                            {"code": "AAAA2222", "name": "Keyboard", "price": 150.00, "currency": "BRL"}
                          ],
                          "page": {"size": 1, "number": 0, "totalElements": 1, "totalPages": 1}
                        }
                        """, MediaType.APPLICATION_JSON));

        List<CatalogProduct> products = catalogClient.getProducts(List.of("AAAA2222"));

        assertThat(products).containsExactly(
                new CatalogProduct("AAAA2222", "Keyboard", new BigDecimal("150.00"), Currency.getInstance("BRL")));
    }

    @Test
    void failsWithBadGatewayWhenResponseIsEmpty() {
        server.expect(requestTo("http://catalog-service/api/products?codes=AAAA2222&size=1"))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> catalogClient.getProducts(List.of("AAAA2222")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
