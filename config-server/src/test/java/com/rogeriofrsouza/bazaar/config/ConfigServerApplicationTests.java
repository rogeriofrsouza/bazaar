package com.rogeriofrsouza.bazaar.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ConfigServerApplicationTests {

    @Value("${local.server.port}")
    private int port;

    @Test
    void servesServiceAndSharedConfigFromConfigRepo() {
        Environment environment = RestClient.create()
                .get()
                .uri("http://localhost:{port}/catalog-service/default", port)
                .retrieve()
                .body(Environment.class);

        assertThat(environment).isNotNull();
        assertThat(environment.getPropertySources())
                .extracting(PropertySource::getName)
                .satisfiesExactly(
                        name -> assertThat(name).endsWith("config-repo/catalog-service.yaml"),
                        name -> assertThat(name).endsWith("config-repo/application.yaml"));
        assertThat(environment.getPropertySources().getFirst().getSource().get("server.port")).isEqualTo(8081);
    }

}
