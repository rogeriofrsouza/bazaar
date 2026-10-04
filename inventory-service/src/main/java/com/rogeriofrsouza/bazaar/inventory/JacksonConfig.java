package com.rogeriofrsouza.bazaar.inventory;

import io.hypersistence.tsid.TSID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.std.FromStringDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Configuration(proxyBeanMethods = false)
class JacksonConfig {

    @Bean
    JacksonModule tsidModule() {
        return new SimpleModule("tsid")
                .addDeserializer(TSID.class, new FromStringDeserializer<>(TSID.class) {
                    @Override
                    protected TSID _deserialize(String value, DeserializationContext ctxt) {
                        return TSID.from(value);
                    }
                })
                .addSerializer(TSID.class, ToStringSerializer.instance);
    }
}
