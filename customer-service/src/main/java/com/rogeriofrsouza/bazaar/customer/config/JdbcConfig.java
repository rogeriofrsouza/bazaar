package com.rogeriofrsouza.bazaar.customer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

@Configuration(proxyBeanMethods = false)
@EnableJdbcAuditing
class JdbcConfig {
}
