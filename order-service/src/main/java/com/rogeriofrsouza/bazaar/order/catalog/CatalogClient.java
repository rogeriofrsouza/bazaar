package com.rogeriofrsouza.bazaar.order.catalog;

import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CatalogClient {

    private final RestClient restClient;

    CatalogClient(RestClient.Builder restClientBuilder, DeferringLoadBalancerInterceptor loadBalancerInterceptor) {
        this.restClient = restClientBuilder
                .baseUrl("http://catalog-service")
                .requestInterceptor(loadBalancerInterceptor)
                .build();
    }

    public CatalogProduct getProduct(String code) {
        return restClient.get()
                .uri("/api/products/{code}", code)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (_, _) -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product " + code + " not found");
                })
                .body(CatalogProduct.class);
    }
}
