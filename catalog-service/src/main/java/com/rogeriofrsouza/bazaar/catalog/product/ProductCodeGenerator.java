package com.rogeriofrsouza.bazaar.catalog.product;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
class ProductCodeGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LENGTH = 8;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final ProductRepository productRepository;

    ProductCodeGenerator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!productRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique product code after " + MAX_ATTEMPTS + " attempts");
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(ALPHABET.length());
            code.append(ALPHABET.charAt(index));
        }
        return code.toString();
    }
}
