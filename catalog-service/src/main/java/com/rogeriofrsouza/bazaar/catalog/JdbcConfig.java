package com.rogeriofrsouza.bazaar.catalog;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

@Configuration(proxyBeanMethods = false)
@EnableJdbcAuditing
class JdbcConfig extends AbstractJdbcConfiguration {
}
