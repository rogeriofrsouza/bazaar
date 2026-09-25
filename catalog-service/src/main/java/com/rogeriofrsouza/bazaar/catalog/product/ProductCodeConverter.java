package com.rogeriofrsouza.bazaar.catalog.product;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class ProductCodeConverter implements AttributeConverter<ProductCode, String> {

    @Override
    public String convertToDatabaseColumn(ProductCode code) {
        return code == null ? null : code.value();
    }

    @Override
    public ProductCode convertToEntityAttribute(String value) {
        return value == null ? null : new ProductCode(value);
    }
}
