package com.rogeriofrsouza.bazaar.inventory;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI openApi(@Value("${bazaar.gateway-url}") String gatewayUrl) {
        return new OpenAPI()
                .info(new Info().title("Inventory API").version("v1"))
                .addServersItem(new Server().url(gatewayUrl).description("API gateway"));
    }
}
