package com.rogeriofrsouza.bazaar.catalog;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

import java.util.Currency;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableJdbcAuditing
class JdbcConfig extends AbstractJdbcConfiguration {

    @Override
    protected List<?> userConverters() {
        return List.of(new CurrencyToStringConverter(), new StringToCurrencyConverter());
    }

    @WritingConverter
    static class CurrencyToStringConverter implements Converter<Currency, String> {

        @Override
        public String convert(Currency source) {
            return source.getCurrencyCode();
        }
    }

    @ReadingConverter
    static class StringToCurrencyConverter implements Converter<String, Currency> {

        @Override
        public Currency convert(String source) {
            return Currency.getInstance(source);
        }
    }
}
