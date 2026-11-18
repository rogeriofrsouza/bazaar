package com.rogeriofrsouza.bazaar.order.catalog;

import com.rogeriofrsouza.bazaar.order.PagedResponse;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class CatalogClient {

    private final RestClient restClient;

    CatalogClient(RestClient.Builder restClientBuilder, DeferringLoadBalancerInterceptor loadBalancerInterceptor) {
        this.restClient = restClientBuilder
                .baseUrl("http://catalog-service")
                .requestInterceptor(loadBalancerInterceptor)
                .build();
    }

    public List<CatalogProduct> getProducts(Collection<UUID> ids) {
        PagedResponse<CatalogProduct> page;
        try {
            page = restClient.get()
                    .uri(builder -> builder.path("/api/products")
                            .queryParam("ids", ids)
                            .queryParam("size", ids.size())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (ResourceAccessException | IllegalStateException ex) {
            // IllegalStateException: the load balancer found no registered catalog-service instance.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Catalog service is unavailable", ex);
        } catch (HttpServerErrorException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Catalog service failed with status " + ex.getStatusCode().value(), ex);
        }

        if (page == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Catalog service returned an empty response");
        }
        return page.content();
    }
}
