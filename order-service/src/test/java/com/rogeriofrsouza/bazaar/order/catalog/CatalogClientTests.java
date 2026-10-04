package com.rogeriofrsouza.bazaar.order.catalog;

import com.rogeriofrsouza.bazaar.order.JacksonConfig;
import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

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
        JsonMapper mapper = JsonMapper.builder()
                .addModule(new JacksonConfig().tsidModule())
                .build();
        RestClient.Builder builder = RestClient.builder()
                .configureMessageConverters(converters -> converters
                        .withJsonConverter(new JacksonJsonHttpMessageConverter(mapper)));
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
        TSID id = TSID.Factory.getTsid();
        server.expect(requestTo("http://catalog-service/api/products?ids=" + id + "&size=1"))
                .andRespond(withSuccess("""
                        {
                          "content": [
                            {"id": "%s", "name": "Keyboard", "price": 150.00, "currency": "BRL"}
                          ],
                          "page": {"size": 1, "number": 0, "totalElements": 1, "totalPages": 1}
                        }
                        """.formatted(id), MediaType.APPLICATION_JSON));

        List<CatalogProduct> products = catalogClient.getProducts(List.of(id));

        assertThat(products).containsExactly(
                new CatalogProduct(id, "Keyboard", new BigDecimal("150.00"), Currency.getInstance("BRL")));
    }

    @Test
    void failsWithBadGatewayWhenResponseIsEmpty() {
        TSID id = TSID.Factory.getTsid();
        server.expect(requestTo("http://catalog-service/api/products?ids=" + id + "&size=1"))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> catalogClient.getProducts(List.of(id)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
