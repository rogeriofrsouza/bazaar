package com.rogeriofrsouza.bazaar.order.catalog;

import com.rogeriofrsouza.bazaar.order.PagedResponse;
import io.hypersistence.tsid.TSID;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;

@Component
public class CatalogClient {

    private final RestClient restClient;

    CatalogClient(RestClient.Builder restClientBuilder, DeferringLoadBalancerInterceptor loadBalancerInterceptor) {
        this.restClient = restClientBuilder
                .baseUrl("http://catalog-service")
                .requestInterceptor(loadBalancerInterceptor)
                .build();
    }

    public List<CatalogProduct> getProducts(Collection<TSID> ids) {
        PagedResponse<CatalogProduct> page = restClient.get()
                .uri(builder -> builder.path("/api/products")
                        .queryParam("ids", ids)
                        .queryParam("size", ids.size())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (page == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Catalog service returned an empty response");
        }
        return page.content();
    }
}
