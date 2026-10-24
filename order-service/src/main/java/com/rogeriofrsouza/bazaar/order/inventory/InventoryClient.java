package com.rogeriofrsouza.bazaar.order.inventory;

import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

@Component
public class InventoryClient {

    private static final String API_PATH = "/api/inventory";

    private final RestClient restClient;

    InventoryClient(RestClient.Builder restClientBuilder, DeferringLoadBalancerInterceptor loadBalancerInterceptor) {
        this.restClient = restClientBuilder
                .baseUrl("http://inventory-service")
                .requestInterceptor(loadBalancerInterceptor)
                .requestInterceptor(InventoryClient::forwardBearerToken)
                .build();
    }

    public void reserve(ReservationRequest request) {
        post(API_PATH + "/reservations", request);
    }

    public void release(ReservationRequest request) {
        post(API_PATH + "/reservations/release", request);
    }

    private void post(String path, ReservationRequest request) {
        try {
            restClient.post()
                    .uri(path)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)
                    || ex.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                String reason = Optional.ofNullable(ex.getResponseBodyAs(ProblemDetail.class))
                        .map(ProblemDetail::getDetail)
                        .orElse("Not enough stock");
                throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
            }
            throw new InventoryUnavailableException(ex);
        } catch (RestClientException ex) {
            throw new InventoryUnavailableException(ex);
        }
    }

    private static ClientHttpResponse forwardBearerToken(HttpRequest request,
                                                         byte[] body,
                                                         ClientHttpRequestExecution execution) throws IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            request.getHeaders().setBearerAuth(authentication.getToken().getTokenValue());
        }
        return execution.execute(request, body);
    }
}
