package com.rogeriofrsouza.bazaar.gateway;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration(proxyBeanMethods = false)
class ApiDocsRouteConfig {

    // One route for every service: YAML lb:// URIs can't take the service name from the path.
    @Bean
    RouterFunction<ServerResponse> serviceApiDocsRoute() {
        return route("service-api-docs")
                .GET("/{service}/v3/api-docs", http())
                .before(setPath("/v3/api-docs"))
                .filter((request, next) -> lb(request.pathVariable("service")).filter(request, next))
                .build();
    }
}
